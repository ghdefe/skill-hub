/**
 * Download command generation utilities.
 *
 * Pure functions that produce git sparse-checkout commands
 * for downloading individual Skills or entire Skill Groups.
 */

/**
 * Generate a git sparse-checkout command for a single Skill.
 *
 * @param repoUrl  - Full GitHub repository URL (e.g. "https://github.com/user/repo")
 * @param repoName - Repository name used as the clone target directory (e.g. "repo")
 * @param folderPath - Skill folder path inside the repo (e.g. "skills/my-skill")
 * @returns The complete download command string
 */
export function generateSkillCommand(
  repoUrl: string,
  repoName: string,
  folderPath: string,
): string {
  return `git clone --filter=blob:none --sparse --depth=1 ${repoUrl} && cd ${repoName} && git sparse-checkout set ${folderPath}`
}

/**
 * Generate a git sparse-checkout command for a Skill Group (batch download).
 *
 * Joins all ACTIVE Skill folder paths into a single sparse-checkout set command.
 *
 * @param repoUrl     - Full GitHub repository URL
 * @param repoName    - Repository name used as the clone target directory
 * @param folderPaths - Array of folder paths for all ACTIVE Skills in the group
 * @returns The complete batch download command string
 */
export function generateGroupCommand(
  repoUrl: string,
  repoName: string,
  folderPaths: string[],
): string {
  const paths = folderPaths.join(' ')
  return `git clone --filter=blob:none --sparse --depth=1 ${repoUrl} && cd ${repoName} && git sparse-checkout set ${paths}`
}
