<a name="br1"></a>**Description of the CI/CD Travel Commerce Solutions Process**

**Build and deploy modules**

Using the example of this module, consider the process [tcs-module-core](https://github.com/travelcommercesolutions/tcs-module-core)

When you push to the main branch, the pipeline is triggered for the buildand release of the module. After starting the pipeline, we get a githubrelease and a nuget package that is stored in github package storage.[Build-and-release.yml](https://github.com/travelcommercesolutions/tcs-module-core/blob/dev/.github/workflows/build-and-release.yml)

To run the pipeline, you need to use the correct syntax [conventiona](https://www.conventionalcommits.org/en/v1.0.0/)[l](https://www.conventionalcommits.org/en/v1.0.0/)[commits](https://www.conventionalcommits.org/en/v1.0.0/).

Example: git commit -m “**fix:** updated module” or “**feat:** added new module”If you need to use a different version of the module in the dependencies,you need to change the version in the module.json and

project\_name.csproj. Choose from [package](https://github.com/orgs/travelcommercesolutions/packages)[ ](https://github.com/orgs/travelcommercesolutions/packages)[storage](https://github.com/orgs/travelcommercesolutions/packages). And to add a newmodule, change the module.json and project\_name.csproj and allow theuse of this package in the github package storage [settings](https://github.com/orgs/travelcommercesolutions/packages/nuget/Travel.Module.Platform.Core/settings). By specifying alink to the dependent repository.

A manual launch has been created for the deployment with the choice ofthe deployment version.




<a name="br2"></a>This action makes changes to modules.json. After that, you need to go tothe platform and update the module



<a name="br3"></a>**Build and deploy storefront**

When you push to the main branch, the pipeline is triggered for the buildand release. After starting the pipeline, we get a github release.[Build-and-release.yml](https://github.com/travelcommercesolutions/tcs-storefront/blob/dev/.github/workflows/build-and-release.yml)

To run the pipeline, you need to use the correct syntax [conventiona](https://www.conventionalcommits.org/en/v1.0.0/)[l](https://www.conventionalcommits.org/en/v1.0.0/)[commits](https://www.conventionalcommits.org/en/v1.0.0/).

Example: git commit -m “**fix:** updated module” or “**feat:** added new module”

A manual launch has been created for the deployment with the choice ofthe deployment version.

At the moment, the tests are organized as a separate action due to the factthat they fall and this will block Build-and-release storefront
