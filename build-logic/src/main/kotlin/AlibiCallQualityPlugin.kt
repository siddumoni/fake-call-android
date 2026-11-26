import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

class AlibiCallQualityPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("verifyNoHardcodedComposeColors") {
            group = "verification"
            description = "Rejects direct Color(...) literals in Compose Kotlin sources."
            doLast {
                val sourceFiles = project.fileTree(project.rootDir) {
                    include("app/src/**/*.kt")
                    exclude("**/build/**")
                }
                val violations = sourceFiles.files.flatMap { file ->
                    file.readLines().mapIndexedNotNull { index, line ->
                        if (Regex("""\bColor\s*\(""").containsMatchIn(line)) {
                            "${file.relativeTo(project.rootDir)}:${index + 1}: $line"
                        } else null
                    }
                }
                check(violations.isEmpty()) {
                    "Hardcoded Compose Color(...) usage is forbidden. Use central theme tokens:\n" +
                        violations.joinToString("\n")
                }
            }
        }
    }
}
