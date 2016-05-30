package net.fybertech.exitpatch;

import java.io.File;
import java.util.List;

import javax.xml.ws.ServiceMode;

import net.fybertech.meddle.MeddleMod;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

@MeddleMod(id="exitpatch", name="ExitPatch", version="1.0", author="FyberOptic", depends={"dynamicmappings"})
public class ExitPatch implements ITweaker {

	@Override
	public void acceptOptions(List<String> arg0, File arg1, File arg2, String arg3) {		
	}

	@Override
	public String[] getLaunchArguments() {
		return new String[0];
	}

	@Override
	public String getLaunchTarget() {
		return null;
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader arg0) {
		arg0.registerTransformer(ExitPatchTransformer.class.getName());		
	}

}
