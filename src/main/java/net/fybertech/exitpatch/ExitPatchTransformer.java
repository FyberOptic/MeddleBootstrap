package net.fybertech.exitpatch;

import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.minecraft.launchwrapper.IClassTransformer;

/**
 * This transformer patches calls to System.exit in net.minecraft.client.Minecraft.
 * Without doing so, Forge's security manager would cause problems when exiting 
 * the game, because it expects to find the deobfuscated class name, where as Meddle 
 * runs with obfuscated names.
 * 
 * It would probably be faster to do this in the constant pool, but this works for 
 * now.
 */
public class ExitPatchTransformer implements IClassTransformer {

	String minecraftClass = null;	
	boolean dmLoaded = false;
	
	public ExitPatchTransformer() {
		minecraftClass = DynamicMappings.getClassMapping("net/minecraft/client/Minecraft");
		System.out.println("Minecraft class: " + minecraftClass);
	}
	
	@Override
	public byte[] transform(String obfClassName, String deobfClassName, byte[] rawClassData) 
	{
		if (!obfClassName.equals(minecraftClass)) return rawClassData;		
	
		ClassReader reader = new ClassReader(rawClassData);
		ClassNode cn = new ClassNode();
		reader.accept(cn, 0);			
		
		for (MethodNode mn : (List<MethodNode>)cn.methods) {
			List<MethodInsnNode> mins = DynamicMappings.getAllInsnNodesOfType(mn.instructions.getFirst(), MethodInsnNode.class);
			for (MethodInsnNode min : mins) {
				if (min.owner.equals("java/lang/System") && min.name.equals("exit")) {
					min.owner = "net/minecraftforge/fml/fmlexitpatch/FMLExitPatch";					
				}
			}
		}
				
		ClassWriter writer = new ClassWriter(0);
		cn.accept(writer);
		return writer.toByteArray();	
	}

}
