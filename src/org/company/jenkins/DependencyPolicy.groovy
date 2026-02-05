package org.company.jenkins

/**
 * Central policy layer.
 * Defines allowed parameter combinations per child job.
 * This file is pure policy: no Jenkins APIs, easy to audit.
 */
class DependencyPolicy {

    /*
     * JOB_POLICIES structure:
     *
     * JOB_NAME:
     *   PARAM_A:
     *     VALUE_1:
     *       PARAM_B: [ALLOWED_VALUES]
     */
    static Map JOB_POLICIES = [

        'CHILD_JOB_DISABLE_DCE_TRAFFIC': [
            APP: [
                APP_A: [ENV: ['PROD_A']],
                APP_B: [ENV: ['PROD_B']]
            ]
        ],

        'CHILD_JOB_DEPLOY_DCE': [
            APP: [
                APP_A: [ENV: ['PROD_A']],
                APP_B: [ENV: ['PROD_B']]
            ]
        ],

        'CHILD_JOB_ENABLE_DCE_TRAFFIC': [
            APP: [
                APP_A: [ENV: ['PROD_A']],
                APP_B: [ENV: ['PROD_B']]
            ]
        ]
    ]

    /**
     * Validate parameters for a specific job.
     * Fails fast with clear errors on policy violation.
     */
    static void validate(String jobName, Map values) {

        if (!JOB_POLICIES.containsKey(jobName)) {
            // No policy defined → allow execution
            return
        }

        def policy = JOB_POLICIES[jobName]

        if (!values.APP || !policy.APP.containsKey(values.APP)) {
            throw new IllegalArgumentException(
                "Invalid APP '${values.APP}' for job ${jobName}"
            )
        }

        if (values.ENV) {
            def allowedEnvs = policy.APP[values.APP].ENV
            if (!allowedEnvs.contains(values.ENV)) {
                throw new IllegalArgumentException(
                    """
Invalid ENV selection for job ${jobName}
APP : ${values.APP}
ENV : ${values.ENV}
Allowed ENV(s): ${allowedEnvs}
""".trim()
                )
            }
        }
    }
}
