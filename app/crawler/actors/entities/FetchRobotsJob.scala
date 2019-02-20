package crawler.actors.entities

import crawler.utils.RobotsExclusion

case class FetchRobotsJob(task: Task)

case class DownloadedRobots(task: Task, robots: RobotsExclusion)
