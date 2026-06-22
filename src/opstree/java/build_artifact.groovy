package opstree.java

import opstree.java.build_artifact
import opstree.common.*

def build_factory(Map step_params) {
    logger = new logger()
    if (step_params.perform_code_build == 'true') {
        build_artifact(step_params)
    } else {
        logger.logger('msg':'No valid option selected for Building Artifact. Please mention correct values.', 'level':'WARN')
    }
}

def build_artifact(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Build Step', 'level':'INFO')

    repo_url = "${step_params.repo_url}"
    perform_code_build = "${step_params.perform_code_build}"
    source_code_path = "${step_params.source_code_path}"
    java_version = "${step_params.java_version}"
    gradle_command = "${step_params.gradle_command}"
    gradle_build_file_location = "${step_params.gradle_build_file_location}"

    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    dir("${WORKSPACE}/${repo_dir}") {
        // Gradle build tool support
        if (java_version == '11') {
            sh """ docker run --rm -v ~/.gradle:/root/.gradle -v ${WORKSPACE}/${repo_dir}:/app/ -w /app gradle:7.5-jdk11 sh -c "cd /app/${gradle_build_file_location} && gradle ${gradle_command}" """
            logger.logger('msg':'Build successful', 'level':'INFO')
        } else if (java_version == '17') {
            sh """ docker run --rm -v ~/.gradle:/root/.gradle -v ${WORKSPACE}/${repo_dir}:/app/ -w /app gradle:7.5-jdk17 sh -c "cd /app/${gradle_build_file_location} && gradle ${gradle_command}" """
            logger.logger('msg':'Build successful', 'level':'INFO')
        } else {
            logger.logger('msg':"Unsupported Java version: ${java_version} for Gradle build", 'level':'ERROR')
            error("Unsupported Java version")
        }
    }
}
