#MeddleBootstrap

The [Meddle](https://github.com/FyberOptic/Meddle) mod loader needs to be specified as a tweak class when launching Minecraft, and its jar needs to be in the classpath.  Unfortunately, there are some Minecraft launchers which don't allow this degree of control.  Some launchers also require you to use the Forge directory structure, and while Meddle can have custom folders specified, they are done so via command line parameters, which goes back to our initial problem in environments where we lack control.

MeddleBootstrap solves this problem by using Forge as a stepping stone.  Forge begins the mod loading process, brings MeddleBootstrap into memory, where it immediately removes Forge's remaining tweak classes to prevent further initialization (such as remapping, which we don't want), and inserts a wrapper class for Meddle instead.  The wrapper allows us to override initialization events, such as for setting the folders to use for mods and configs.

Elements of Forge will remain, such as its custom logger and security manager, but these won't affect operation much.  A patch is included as a workaround to the security manager to allow for graceful exits, since the lack of remapping confuses it.

MeddleBootstrap needs to build with a version of Meddle inside of it.  Version 1.3.1 or higher is required.  The specific jar can be specified in the Gradle build file, where its contents will be copied into the final jar automatically.


