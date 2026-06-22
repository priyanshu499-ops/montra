package opstree.java

import opstree.common.*

def unit_testing_factory(Map step_params) {
    def logger = new logger()
    logger.logger('msg':"DEBUG unit_testing_factory received unit_testing_check=[${step_params.unit_testing_check}] type=${step_params.unit_testing_check?.getClass()}", 'level':'INFO')
    if (step_params.unit_testing_check == 'true') {
        unit_test(step_params)
    } else {
        logger.logger('msg':'No valid option selected for Unit Testing. Please mention correct values.', 'level':'WARN')
    }
}

def unit_test(Map step_params) {
    def logger = new logger()
    def parser = new parser()
    def reports_manager = new reports_management()

    logger.logger('msg':'Performing Unit Tests', 'level':'INFO')

    def repo_url                        = step_params.repo_url?.toString()
    def repo_url_type                   = step_params.repo_url_type?.toString()
    def unit_testing_check              = step_params.unit_testing_check?.toString()
    def fail_job_if_unit_issue_detected = step_params.fail_job_if_unit_issue_detected?.toString()
    def unit_test_reports_path          = step_params.unit_test_reports_path?.toString()
    def findbugs_test_report_path       = step_params.findbugs_test_report_path?.toString()
    def source_code_path                = step_params.source_code_path?.toString()
    def java_version                    = step_params.java_version?.toString()

    logger.logger('msg':"DEBUG java_version=[${java_version}] fail_job=[${fail_job_if_unit_issue_detected}]", 'level':'INFO')

    def repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    dir("${WORKSPACE}/${repo_dir}") {
        if (fail_job_if_unit_issue_detected == 'false') {
            try {
                if (java_version == '11') {
                    sh """ docker run --rm -v ~/.gradle:/root/.gradle -v ${WORKSPACE}/${repo_dir}:/app -w /app gradle:7.5-jdk11 sh -c 'gradle test' """
                } else if (java_version == '17') {
                    sh """ docker run --rm -v ~/.gradle:/root/.gradle -v ${WORKSPACE}/${repo_dir}:/app -w /app gradle:7.5-jdk17 sh -c 'gradle test' """
                } else {
                    logger.logger('msg':"Unsupported Java version: ${java_version} for Gradle Unit Test", 'level':'ERROR')
                    error("Unsupported Java version")
                }
                reports_manager.publish_static_code_analysis_issues(unit_test_reports_path: "${unit_test_reports_path}", findbugs_test_report_path: "${findbugs_test_report_path}")
            } catch (Exception e) {
                logger.logger('msg':'Gradle Unit Test found Issues!! Ignoring as per User inputs', 'level':'WARN')
            }
        } else {
            try {
                if (java_version == '11') {
                    sh """ docker run --rm -v ~/.gradle:/root/.gradle -v ${WORKSPACE}/${repo_dir}:/app -w /app gradle:7.5-jdk11 sh -c 'gradle test' """
                } else if (java_version == '17') {
                    sh """ docker run --rm -v ~/.gradle:/root/.gradle -v ${WORKSPACE}/${repo_dir}:/app -w /app gradle:7.5-jdk17 sh -c 'gradle test' """
                } else {
                    logger.logger('msg':"Unsupported Java version: ${java_version} for Gradle Unit Test", 'level':'ERROR')
                    error("Unsupported Java version")
                }
                reports_manager.publish_static_code_analysis_issues(unit_test_reports_path: "${unit_test_reports_path}", findbugs_test_report_path: "${findbugs_test_report_path}")
            } catch (Exception e) {
                logger.logger('msg':"Gradle Unit Test Failed Error Details: ${e}", 'level':'ERROR')
                error()
            }
        }
    }
}
