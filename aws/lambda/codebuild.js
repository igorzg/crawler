const AWS = require("aws-sdk");
const AWS_CODE_BUILD = new AWS.CodeBuild();
const AWS_CLOUDWATCH_EVENT = new AWS.CloudWatchEvents({apiVersion: '2015-10-07'});
const CFN = require("./cloudformation");

const CREATE = "CREATE";
const DELETE = "DELETE";
const SUCCESS = "SUCCESS";
const FAILED = "FAILED";
const EVENT_CODE_BUILD = "aws.codebuild";
const EVENT_CFN = "CLOUD_FORMATION";

class AwsEventHandler {


    /**
     * Event handler
     * @param event
     * @param context
     * @param callback
     */
    constructor(event, context, callback) {
        this.event = event;
        this.context = context;
        this.callback = callback;
        console.log("RECEIVED EVENT", this.event);
    }

    /**
     * Resolver
     */
    resolve() {
        if (this.event.source === EVENT_CODE_BUILD) {
            this.doListRules().then(
                data => {
                    if (!data && data.length === 0) {
                        this.callback(new Error("Missing rule event, cannot start codebuild!"));
                    } else {
                        this.doCodeBuildFeedback();
                    }
                },
                err => this.callback(err)
            );
        } else if (this.event.RequestType.toUpperCase() === CREATE) {
            this.doStartCodeBuild();
        } else if (this.event.RequestType.toUpperCase() === DELETE) {
            this.doStackFeedBack(SUCCESS, {});
        } else {
            this.callback(new Error("Unknown event type"))
        }
    }

    /**
     * codeBuild feedback
     */
    doCodeBuildFeedback() {
        const event = this.event;
        const build = event.detail;
        const cfnEvent = build["additional-information"].environment["environment-variables"]
            .find(item => item.name === EVENT_CFN);
        let status = FAILED;
        if (build["build-status"] === "SUCCEEDED") {
            status = SUCCESS;
        }
        try {
            this.event = JSON.parse(this.fromBase64(cfnEvent));
            console.log("NOTIFY STACK SUCCEEDED", this.event);
            this.doStackFeedBack(status, {
                "build-id": build["build-id"],
                "build-status": build["build-status"],
                "project-name": build["project-name"],
                time: event.time,
                id: event.id,
                source: event.source,
                region: event.region,
                account: event.account
            });
        } catch (e) {
            console.error("Error parsing json", e);
            console.log(this.event);
        }
    }

    /**
     * List all cloud watch event rules
     */
    doListRules() {
        const eventName = this.event.ResourceProperties.StackEventName;
        return new Promise((resolve, reject) => {
            AWS_CLOUDWATCH_EVENT.listRules({
                NamePrefix: eventName
            }, function (err, data) {
                if (err) {
                    console.error("List rules error", err);
                    reject(err);
                } else {
                    console.log("List rules", data);
                    resolve(data);
                }
            });
        });
    }

    /**
     * Start code build job
     */
    doStartCodeBuild() {
        const stackName = this.event.ResourceProperties.StackName;

        AWS_CODE_BUILD.startBuild({
            projectName: stackName,
            environmentVariablesOverride: [
                {
                    name: EVENT_CFN,
                    value: this.toBase64(this.event)
                }
            ]
        }, (err, data) => {
            if (err) {
                console.error("CodeBuild cannot be started", err);
                this.callback(err);
            } else {
                console.log("CodeBuild started", data);
                this.callback(null, JSON.stringify(data));
            }
        });
    }

    /**
     * Send feedback to cloudformation
     * @param status
     * @param data
     */
    doStackFeedBack(status, data) {
        CFN.doStackFeedback(this.event, status, data)
            .then(body => {
                console.log("CloudFormation", body);
                this.callback(null,
                    {
                        EVENT: "CloudFormation",
                        result: body,
                        event: this.event
                    }
                );
            }, err => {
                console.error("Failed to send feedback to CloudFormation", err);
                console.log(this.event);
                this.callback(err);
            });
    }

    /**
     * From base 64 to string
     * @param data
     * @returns {*}
     */
    fromBase64(data) {
        return Buffer.from(data, "base64").toString("ascii");
    }

    /**
     * To base 64
     * @param data
     * @returns {string|*|*}
     */
    toBase64(data) {
        return new Buffer(JSON.stringify(data)).toString("base64");
    }
}

exports.handler = (event, context, callback) => new AwsEventHandler(event, context, callback).resolve();