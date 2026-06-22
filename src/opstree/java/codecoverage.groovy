package opstree.java

import opstree.java.codecoverage

import opstree.common.*

def code_coverage_factory(Map step_params) {
    logger = new logger()
    if (step_params.perform_code_coverage == 'true') {
        code_coverage(step_params)
    } else {
        logger.logger('msg':'No valid option selected for code coverage. Please mention correct values.', 'level':'WARN')
    }
}

def code_coverage(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Code coverage Step', 'level':'INFO')

    repo_url = "${step_params.repo_url}"
    repo_url_type = "${step_params.repo_url_type}"

    code_coverage_test = "${step_params.code_coverage_test}"
    source_code_path = "${step_params.source_code_path}"
    java_version = "${step_params.java_version}"
    source_code_path = "${step_params.source_code_path}"

    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")
    repo_dir = repo_dir + source_code_path

    try {
        dir("${WORKSPACE}/${repo_dir}") {
            if (java_version == '11') {
                docker_image = 'gradle:7.5-jdk11'
            } else if (java_version == '17') {
                docker_image = 'gradle:7.5-jdk17'
            } else {
                logger.logger('msg':"Unsupported Java version: ${java_version} for Code Coverage", 'level':'ERROR')
                error("Unsupported Java version")
            }

            sh """
                docker run --rm \
                    -v ~/.gradle:/root/.gradle \
                    -v ${WORKSPACE}/${repo_dir}:/app \
                    -w /app \
                    ${docker_image} \
                    sh -c "gradle test jacocoTestReport"
            """
            logger.logger('msg':'Code coverage successful', 'level':'INFO')
        }
    } catch (Exception e) {
        logger.logger('msg':"Code Coverage Failed Error Details: ${e.message}", 'level':'ERROR')
        error('Build process failed due to an exception.')
    }
}
