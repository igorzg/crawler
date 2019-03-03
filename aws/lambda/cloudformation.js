const https = require("https");
const url = require("url");

const DELETE = "DELETE";
/**
 * Do stack feedback request
 * @param event
 * @param status
 * @param data
 * @returns {Promise<any>}
 */
exports.doStackFeedback = (event, status, data) => {
    return new Promise((resolve, reject) => {
        const body = {
            Status: status,
            RequestId: event.RequestId,
            LogicalResourceId: event.LogicalResourceId,
            StackId: event.StackId,
            PhysicalResourceId: event.PhysicalResourceId
        };

        if (this.event.RequestType.toUpperCase() !== DELETE) {
            if (status === "FAILED") {
                body.Reason = data;
            } else {
                body.Data = data;
            }
        }

        console.log("DO STACK FEEDBACK");
        console.log(body);

        const sBody = JSON.stringify(body);
        const responseURL = url.parse(event.ResponseURL);
        const options = {
            hostname: responseURL.hostname,
            port: 443,
            path: responseURL.path,
            method: "PUT",
            headers: {
                "content-type": "",
                "content-length": event.ResponseURL.length,
            }
        };
        const request = https.request(options, response => {
            let chunks = [];
            response.on("data", data => chunks.push(new Buffer(data, "ascii")));
            response.on("end", () => {
               resolve(chunks.map(buffer => buffer.toString("utf8")).join(""));
            });
        });
        request.on("error", err => reject(err));
        request.write(sBody);
        request.end();
    });
};