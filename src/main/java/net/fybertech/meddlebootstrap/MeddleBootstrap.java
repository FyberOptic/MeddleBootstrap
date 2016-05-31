package net.fybertech.meddlebootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.fybertech.meddle.Meddle.ModContainer;
import net.fybertech.meddle.MeddleUtil;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;



public class MeddleBootstrap implements ITweaker 
{

	/**
	 * This dummy tweak class is used to replace any class we remove.
	 */
	public static class PlaceholderTweak implements ITweaker
	{
		@Override
		public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
		}

		@Override
		public void injectIntoClassLoader(LaunchClassLoader classLoader) {
		}

		@Override
		public String getLaunchTarget() {			
			return null;
		}

		@Override
		public String[] getLaunchArguments() {
			return new String[0];
		}
		
	}
	
		
	
	
	/**
	 *  This class wraps the Meddle tweak so that we can control it better during 
	 *  the bootstrapping process.
	 */
	public static class WrapperTweak implements ITweaker
	{
		ITweaker meddle;
		
		public WrapperTweak() throws InstantiationException, IllegalAccessException, ClassNotFoundException
		{
			// Initialize Meddle here as if it had been done by launchwrapper 
			Launch.classLoader.addClassLoaderExclusion("net.fybertech.meddle");
			meddle = (ITweaker) Class.forName("net.fybertech.meddle.Meddle", true, Launch.classLoader).newInstance();			
			
			// Set this in case we ever need to reference it in Meddle mods.
			net.fybertech.meddle.Meddle.isBootstrapped = true;
			
			// Add this class to Meddle's blacklist to prevent it from loading this jar again later, 
			// which will happen since the MeddleBootstrap mod goes in the same folder that the
			// actual Meddle mods will go.
			net.fybertech.meddle.Meddle.blacklistedTweaks.add("net.fybertech.meddlebootstrap.MeddleBootstrap");
		}
		
		
		@Override
		public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) 
		{
			System.out.println("Overriding Meddle directories for bootstrap");
			
			// We're changing these directories to match Forge's layout to be compatible with 
			// typical launchers.
			List<String> newArgs = new ArrayList<String>();
			newArgs.addAll(args);
			newArgs.add("--meddledir");
			newArgs.add("mods/");
			newArgs.add("--meddleconfigdir");
			newArgs.add("config/");
			
			meddle.acceptOptions(newArgs, gameDir, assetsDir, profile);

			
			// Install a patch to all System.exit calls in net.minecraft.client.Minecraft to 
			// avoid Forge's security manager going nuts when the game exits.			
			if (MeddleUtil.isClientJar()) {
				Class<? extends ITweaker> tweakClass = null;
				try {
					tweakClass = (Class<? extends ITweaker>) Class.forName("net.fybertech.exitpatch.ExitPatch", false, Launch.classLoader);		
				} catch (Exception e) {
					System.out.println("Error: Couldn't load ExitPatch class");
				}
				
				if (tweakClass != null) {
					ModContainer mod = new ModContainer(null);
					mod.tweakClass = tweakClass;
					mod.meta = null;
					mod.id = "exitpatch";
					net.fybertech.meddle.Meddle.loadedModsList.put(mod.id, mod);
					System.out.println("Added ExitPatch to mods list");
				}
			}
		}

		@Override
		public void injectIntoClassLoader(LaunchClassLoader classLoader) {
			meddle.injectIntoClassLoader(classLoader);
		}

		@Override
		public String getLaunchTarget() {			
			return null;
		}

		@Override
		public String[] getLaunchArguments() {
			
			// Remove trailing "/Forge" from Minecraft version on title screen and logs
			List<String> args = (List<String>) Launch.blackboard.get("ArgumentList");			
			for (Iterator<String> it = args.iterator(); it.hasNext();) {
				String s = it.next();
				//System.out.println(s);
				if (s.toLowerCase().equals("--versiontype")) {
					it.remove();
					it.next();
					it.remove();
				}
			}
			
			return new String[0];
		}		
	}
	
	
	
	
	
	/**
	 *  Cleans all tweaks from being loaded
	 */
	public static void cleanTweaks()
	{
		List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");
		for (Iterator<String> it = tweakClasses.iterator(); it.hasNext();) {
			String s = it.next();
			if (!s.startsWith(MeddleBootstrap.class.getName())) {			
				System.out.println("Removing TweakClass " + s);				
				Collections.replaceAll(tweakClasses, s, PlaceholderTweak.class.getName());
			}
		}
		
		List<ITweaker> tweaks = (List<ITweaker>) Launch.blackboard.get("Tweaks");	
		for (int n = 0; n < tweaks.size(); n++) {
			ITweaker tweak = tweaks.get(n);
			if (!tweak.toString().startsWith(MeddleBootstrap.class.getName())) 
			{			
				System.out.println("Removing Tweak " + tweak.toString());
				Collections.replaceAll(tweaks, tweak, new PlaceholderTweak());
			}
		}		
	}
	
		
	
	
	
	
	
	public static int loadCount = 0;
	
	public MeddleBootstrap()
	{
		loadCount++;		
		if (loadCount > 1) return;
		
		// Clear out the tweak class lists as soon as possible.
		cleanTweaks();		
	}
	
	
	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) 
	{
		if (loadCount > 1) return;
		
		// Add the Meddle wrapper into the list of tweaks to initialize
		List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");
		tweakClasses.add(WrapperTweak.class.getName());
	}

	
	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) 
	{
	}

	
	@Override
	public String getLaunchTarget() 
	{
		return null;
	}
	

	@Override
	public String[] getLaunchArguments() 
	{			
		return new String[0];
	}

	
}
