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
  const tmp = `.skillhub-tmp-${repoName}`
  const skillName = folderPath.split('/').pop()!
  return `git clone --filter=blob:none --no-checkout --depth=1 ${repoUrl} ${tmp} && cd ${tmp} && git sparse-checkout init --no-cone && git sparse-checkout set ${folderPath} && git checkout && cp -r ${folderPath} ../${skillName} && cd .. && rm -rf ${tmp}`
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
  const tmp = `.skillhub-tmp-${repoName}`
  const paths = folderPaths.join(' ')
  const copyParts = folderPaths
    .map(p => {
      const name = p.split('/').pop()!
      return `cp -r ${p} ../${name}`
    })
    .join(' && ')
  return `git clone --filter=blob:none --no-checkout --depth=1 ${repoUrl} ${tmp} && cd ${tmp} && git sparse-checkout init --no-cone && git sparse-checkout set ${paths} && git checkout && ${copyParts} && cd .. && rm -rf ${tmp}`
}
