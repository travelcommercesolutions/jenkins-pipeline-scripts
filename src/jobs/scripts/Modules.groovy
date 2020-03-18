package jobs.scripts;

class Modules {

    private static String DefaultBranchOrCommitPR = '${sha1}'
    private static String DefaultBranchOrCommitPush = '*/master'
    private static String DefaultRefSpec = '+refs/pull/*:refs/remotes/origin/pr/*'
    private static String DefaultMSBuild = 'MSBuild'  

    def static createModuleArtifact(context, def manifestDirectory)
    {
        def tempDir = Utilities.getTempFolder(context)
        def modulesDir = "$tempDir\\_PublishedWebsites"
        def packagesDir = Utilities.getArtifactFolder(context)

        context.dir(packagesDir)
        {
            context.deleteDir()
        }

        // create artifacts
        context.dir(manifestDirectory)
        {
            def projects = context.findFiles(glob: '*.csproj')
            if (projects.size() > 0) {
                for (int i = 0; i < projects.size(); i++)
                {
                    def project = projects[i]
                    def path = project.name
                    context.bat "\"${context.tool DefaultMSBuild}\" \"${path}\" /nologo /verbosity:m /t:Clean,PackModule /p:Configuration=Release /p:Platform=AnyCPU /p:DebugType=none /p:AllowedReferenceRelatedFileExtensions=.xml \"/p:OutputPath=$tempDir\" \"/p:VCModulesOutputDir=$modulesDir\" \"/p:VCModulesZipDir=$packagesDir\""
                }
            }
        }
    }

    def static installModuleArtifacts(context)
    {
        def packagesDir = Utilities.getArtifactFolder(context)
        def packages;
        context.dir(packagesDir)
        {
            packages = context.findFiles(glob: '*.zip')
        }

        if (packages.size() > 0) {
            for (int i = 0; i < packages.size(); i++)
            {
                Packaging.installModule(context, "${packagesDir}\\${packages[i].path}")
            }
        }
    }

    def static runUnitTests(context)
    {
        Modules.runTests(context, "Category=Unit|Category=CI", "xUnit.UnitTests.xml")
    }

    def static runIntegrationTests(context)
    {
        def platformPort = Utilities.getPlatformPort(context)
        def storefrontPort = Utilities.getStorefrontPort(context)
        def sqlPort = Utilities.getSqlPort(context)

        // create context
        context.withEnv(["VC_PLATFORM=http://ci.virtocommerce.com:${platformPort}", "VC_STOREFRONT=http://ci.virtocommerce.com:${storefrontPort}", "VIRTO_CONN_STR_VirtoCommerce=Data Source=http://ci.virtocommerce.com:${sqlPort};Initial Catalog=VirtoCommerce2;Persist Security Info=True;User ID=sa;Password=v!rto_Labs!;MultipleActiveResultSets=True;Connect Timeout=30" ]) {
            Modules.runTests(context, "Category=Integration", "xUnit.IntegrationTests.xml")
        }
    }

    def static runTests(context, traits, resultsFileName)
    {
        def paths = Utilities.prepareTestEnvironment(context)
        Utilities.runUnitTest(context, traits, paths, resultsFileName)
    }    

    def static getModuleId(context){
        def manifests = context.findFiles(glob: '**\\module.manifest')
        def manifestPath = ""
        if (manifests.size() > 0) {
            for (int i = 0; i < manifests.size(); i++)
            {
                manifestPath = manifests[i].path
            }
        }
        else {
            context.echo "no module.manifest files found"
            return null
        }

        def wsDir = context.env.WORKSPACE
        def fullManifestPath = "$wsDir\\$manifestPath"

        context.echo "parsing $manifestPath"
        def manifestContent = context.readFile(manifestPath)
        def manifest = new XmlSlurper().parseText(manifestContent)  // UTF-8 wo BOM only stream of bytes

        def id = manifest.id.toString()

        return id
    }
}
