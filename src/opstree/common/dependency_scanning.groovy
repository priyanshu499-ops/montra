package opstree.common

import opstree.common.*

def dependency_scanning_factory(Map step_params) {
    def logger = new logger()
    if (step_params.dependency_check == 'true') {
        dependency_scan(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for dependency scanning. Please mention correct values.', 'level':'WARN')
  }
}

def dependency_scan(Map step_params) {
    def logger = new logger()
    def parser = new parser()
    def dependency_check_reports = new reports_management()

    logger.logger('msg':'Performing Dependency Check Scanning', 'level':'INFO')

    def repo_url = "${step_params.repo_url}"
    def repo_url_type = "${step_params.repo_url_type}"

    def dependency_check = "${step_params.dependency_check}"
    def dependency_scan_tool = "${step_params.dependency_scan_tool}"
    def fail_job_if_dependency_returned_exception = "${step_params.fail_job_if_dependency_returned_exception}"

    def owasp_project_name = "${step_params.owasp_project_name}"
    def owasp_report_format = "${step_params.owasp_report_format}"
    def owasp_report_publish = "${step_params.owasp_report_publish}"
    def owasp_version = '10.0.4'
    def source_code_path = "${step_params.source_code_path}"
    def app_stack = "${step_params.app_stack}"
    def nvd_api_key_creds_id = step_params.nvd_api_key_creds_id ?: 'NVD_API_KEY'

    def repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")
    def new_repo_dir = repo_dir + source_code_path

    if (dependency_scan_tool == 'owasp') {
        sh "mkdir -p ${WORKSPACE}/owasp-reports"
        sh "mkdir -p ${JENKINS_HOME}/owasp-data/cache"
        sh "sudo chmod -R 777 ${WORKSPACE}/owasp-reports ${JENKINS_HOME}/owasp-data"

        // Remove stale lock files left by OOM-killed containers from previous runs
        sh "find ${JENKINS_HOME}/owasp-data -name '*.lock' -delete 2>/dev/null || true"

        // Use --noupdate if DB was successfully updated in the last 24 hours
        // This prevents 20+ min full re-downloads on every build
        def noUpdateFlag = sh(script: """
            DB_FILE="${JENKINS_HOME}/owasp-data/odc.mv.db"
            if [ -f "\$DB_FILE" ]; then
                AGE_HOURS=\$(( ( \$(date +%s) - \$(stat -f %m "\$DB_FILE" 2>/dev/null || stat -c %Y "\$DB_FILE" 2>/dev/null || echo 0) ) / 3600 ))
                if [ "\$AGE_HOURS" -lt "24" ] 2>/dev/null; then
                    echo "--noupdate"
                else
                    echo ""
                fi
            else
                echo ""
            fi
        """, returnStdout: true).trim()

        if (noUpdateFlag == '--noupdate') {
            logger.logger('msg':'OWASP DB is fresh (< 24h old) - skipping NVD update for faster scan', 'level':'INFO')
        } else {
            logger.logger('msg':'OWASP DB is stale or missing - downloading NVD updates', 'level':'INFO')
        }

        // Build the docker run command with memory limit to prevent OOM kill (exit 137)
        def owaspDockerBase = "docker run --rm --memory=4g --memory-swap=4g"
        def owaspDataVol = "-v ${JENKINS_HOME}/owasp-data:/usr/share/dependency-check/data:z"
        def owaspReportsVol = "-v ${WORKSPACE}/owasp-reports:/reports:z"

        try {
            // NVD API key is required for dependency-check 10.x+ (NVD retired v1.1 feeds)
            withCredentials([string(credentialsId: nvd_api_key_creds_id, variable: 'NVD_API_KEY')]) {
                def nvdApiKeyArg = "--nvdApiKey \${NVD_API_KEY}"
                def owaspScanArgs = "--format ALL --project '${owasp_project_name}' --out /reports ${noUpdateFlag} ${nvdApiKeyArg}"
                sh "${owaspDockerBase} -v ${WORKSPACE}/${new_repo_dir}:/src:z ${owaspDataVol} ${owaspReportsVol} -e NVD_API_KEY=\${NVD_API_KEY} owasp/dependency-check:${owasp_version} --scan /src ${owaspScanArgs}"
            }
        }
        catch (Exception e) {
            if (fail_job_if_dependency_returned_exception == 'true') {
                logger.logger('msg':"Dependency Scanning Failed: ${e}", 'level':'ERROR')
                throw e
            } else {
                logger.logger('msg':"Dependency Scanning Failed: [IGNORING] ${e}", 'level':'WARN')
            }
        }

        if (owasp_report_publish == 'true') {
            logger.logger('msg':'Publishing OWASP Dependency Scan Report', 'level':'INFO')
            dependency_check_reports.publish('dc_publisher':'true', 'report_dir':"${WORKSPACE}/owasp-reports", 'report_file':"dependency-check-report.${owasp_report_format}", 'report_name':'OWASP Dependency Check Report')
        }
        else {
            logger.logger('msg':'OWASP Report Publishing Skipped', 'level':'INFO')
        }
    }

    else if (dependency_scan_tool == 'fossa') {
        echo 'Fossa will be added soon'
    }

    else {
        logger.logger('msg':'No valid option was selected for scanning tool.', 'level':'ERROR')
    }
}
