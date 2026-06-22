package opstree.common

import opstree.common.*

def snyk_security_scanning_factory(Map step_params) {
    def logger = new logger()
    if (step_params.snyk_security_check == 'true') {
        snyk_security_scan(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for snyk_security scanning. Please mention correct values.', 'level':'WARN')
  }
}

def snyk_security_scan(Map step_params) {
    def logger = new logger()
    def parser = new parser()

    logger.logger('msg':'Performing Snyk Security Check Scanning', 'level':'INFO')

    def repo_url = "${step_params.repo_url}"
    def repo_url_type = "${step_params.repo_url_type}"

    def snyk_security_check = "${step_params.snyk_security_check}"
    def fail_job_if_snyk_security_returned_exception = "${step_params.fail_job_if_snyk_security_returned_exception}"
    def snyk_report_format = "${step_params.snyk_report_format}"
    def snyk_api_creds_id = "${step_params.snyk_api_creds_id}"
    def language_codebase_for_snyk_check = "${step_params.language_codebase_for_snyk_check}"

    def repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    sh "mkdir -p ${WORKSPACE}/snyk-reports"

        // This below line is temp fix.. Need to check alternative

    sh "sudo chmod -R 777 ${WORKSPACE}/snyk-reports "

    if (fail_job_if_snyk_security_returned_exception == true) {
        try {
            withCredentials([string(credentialsId: snyk_api_creds_id , variable: 'SNYK_TOKEN')]) {
                    sh "docker run --rm --env SNYK_TOKEN=${SNYK_TOKEN} -v $WORKSPACE/'${repo_dir}':/app -v $WORKSPACE/snyk-reports:/home/snyk-reports snyk/snyk:'${language_codebase_for_snyk_check}' snyk test --allow-missing --'${snyk_report_format}'-file-output=/home/snyk-reports/snyk_report.'${snyk_report_format}'"
            }
        }
                catch (Exception e) {
            logger.logger('msg':"Snyk Security Scanning Failed Error Details: ${e}", 'level':'ERROR')
                }
    }

            else {
        try {
            withCredentials([string(credentialsId: snyk_api_creds_id , variable: 'SNYK_TOKEN')]) {
                    sh "docker run --rm --env SNYK_TOKEN=${SNYK_TOKEN} -v $WORKSPACE/'${repo_dir}':/app -v $WORKSPACE/snyk-reports:/home/snyk-reports snyk/snyk:'${language_codebase_for_snyk_check}' snyk test --allow-missing --'${snyk_report_format}'-file-output=/home/snyk-reports/snyk_report.'${snyk_report_format}'"
            }
        }
                catch (Exception e) {
            logger.logger('msg':"Snyk Security Scanning Failed: [IGNORING] ${e}", 'level':'WARN')
                }
            }
}
