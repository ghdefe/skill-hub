/**
 * Download command generation utilities.
 *
 * Produces git sparse-checkout commands for downloading individual Skills
 * or entire Skill Groups, with platform-specific variants.
 */

export type ShellType = 'bash' | 'powershell' | 'cmd'

export const shellLabels: Record<ShellType, string> = {
  bash: 'Linux / macOS',
  powershell: 'PowerShell',
  cmd: 'CMD',
}

/**
 * Detect the user's likely shell based on navigator.userAgent.
 */
export function detectShell(): ShellType {
  const ua = navigator.userAgent.toLowerCase()
  if (ua.includes('win')) return 'powershell'
  return 'bash'
}

/**
 * Generate a git sparse-checkout command for a single Skill.
 */
export function generateSkillCommand(
  repoUrl: string,
  repoName: string,
  folderPath: string,
  shell: ShellType = 'bash',
): string {
  const tmp = `.skillhub-tmp-${repoName}`

  // Handle root directory case (empty folderPath)
  const isRootDir = !folderPath || folderPath === '.'
  const skillName = isRootDir ? repoName : folderPath.split('/').pop()!

  // For root directory, clone the entire repo without sparse-checkout
  if (isRootDir) {
    switch (shell) {
      case 'powershell':
        return `git clone --depth=1 ${repoUrl} ${skillName}`
      case 'cmd':
        return `git clone --depth=1 ${repoUrl} ${skillName}`
      default:
        return `git clone --depth=1 ${repoUrl} ${skillName}`
    }
  }

  // For subdirectory, use sparse-checkout
  switch (shell) {
    case 'powershell':
      return `git clone --filter=blob:none --no-checkout --depth=1 ${repoUrl} ${tmp}; cd ${tmp}; git sparse-checkout init --no-cone; git sparse-checkout set ${folderPath}; git checkout; Copy-Item -Recurse ${folderPath.replace(/\//g, '\\')} ..\\${skillName}; cd ..; Remove-Item -Recurse -Force ${tmp}`
    case 'cmd':
      return `git clone --filter=blob:none --no-checkout --depth=1 ${repoUrl} ${tmp} && cd ${tmp} && git sparse-checkout init --no-cone && git sparse-checkout set ${folderPath} && git checkout && xcopy /E /I /Q ${folderPath.replace(/\//g, '\\')} ..\\${skillName}\\ && cd .. && rmdir /s /q ${tmp}`
    default:
      return `git clone --filter=blob:none --no-checkout --depth=1 ${repoUrl} ${tmp} && cd ${tmp} && git sparse-checkout init --no-cone && git sparse-checkout set ${folderPath} && git checkout && cp -r ${folderPath} ../${skillName} && cd .. && rm -rf ${tmp}`
  }
}

/**
 * Generate a git sparse-checkout command for a Skill Group (batch download).
 */
export function generateGroupCommand(
  repoUrl: string,
  repoName: string,
  folderPaths: string[],
  shell: ShellType = 'bash',
): string {
  const tmp = `.skillhub-tmp-${repoName}`

  // Filter out empty paths and normalize
  const validPaths = folderPaths.filter(p => p && p !== '.')

  // If all paths are root or empty, clone the entire repo
  if (validPaths.length === 0) {
    switch (shell) {
      case 'powershell':
        return `git clone --depth=1 ${repoUrl} ${repoName}`
      case 'cmd':
        return `git clone --depth=1 ${repoUrl} ${repoName}`
      default:
        return `git clone --depth=1 ${repoUrl} ${repoName}`
    }
  }

  const paths = validPaths.join(' ')

  switch (shell) {
    case 'powershell': {
      const copyParts = validPaths
        .map(p => {
          const name = p.split('/').pop()!
          return `Copy-Item -Recurse ${p.replace(/\//g, '\\')} ..\\${name}`
        })
        .join('; ')
      return `git clone --filter=blob:none --no-checkout --depth=1 ${repoUrl} ${tmp}; cd ${tmp}; git sparse-checkout init --no-cone; git sparse-checkout set ${paths}; git checkout; ${copyParts}; cd ..; Remove-Item -Recurse -Force ${tmp}`
    }
    case 'cmd': {
      const copyParts = validPaths
        .map(p => {
          const name = p.split('/').pop()!
          return `xcopy /E /I /Q ${p.replace(/\//g, '\\')} ..\\${name}\\`
        })
        .join(' && ')
      return `git clone --filter=blob:none --no-checkout --depth=1 ${repoUrl} ${tmp} && cd ${tmp} && git sparse-checkout init --no-cone && git sparse-checkout set ${paths} && git checkout && ${copyParts} && cd .. && rmdir /s /q ${tmp}`
    }
    default: {
      const copyParts = validPaths
        .map(p => {
          const name = p.split('/').pop()!
          return `cp -r ${p} ../${name}`
        })
        .join(' && ')
      return `git clone --filter=blob:none --no-checkout --depth=1 ${repoUrl} ${tmp} && cd ${tmp} && git sparse-checkout init --no-cone && git sparse-checkout set ${paths} && git checkout && ${copyParts} && cd .. && rm -rf ${tmp}`
    }
  }
}
