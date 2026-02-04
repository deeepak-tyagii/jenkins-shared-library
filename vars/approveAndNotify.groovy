def call(Map config = [:]) {

    def approvers = config.approvers ?: 'ops-team'
    def timeoutMinutes = config.timeout ?: 30
    def serviceName = config.service ?: 'unknown-service'

    timeout(time: timeoutMinutes, unit: 'MINUTES') {
        input message: """
Approve deployment for ${serviceName}?

ENV            : ${params.ENV}
VERSION        : ${params.VERSION}
REGION         : ${params.REGION}
RUN_MIGRATIONS : ${params.RUN_MIGRATIONS}
OWNER          : ${params.OWNER}
""",
        submitter: approvers
    }
}
