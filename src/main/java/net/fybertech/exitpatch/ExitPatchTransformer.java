package net.fybertech.exitpatch;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

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
		minecraftClass = getMinecraftClass();
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
			List<MethodInsnNode> mins = getAllInsnNodesOfType(mn.instructions.getFirst(), MethodInsnNode.class);
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

	
	
	
	
	// Borrow a few things and modify slightly, to remove the DynamicMappings dependency
	
	public static ClassNode getClassNode(String className)
	{
		if (className == null) return null;

		className = className.replace(".", "/");		
		
		InputStream stream = Launch.classLoader.getResourceAsStream(className + ".class");
		if (stream == null) return null;

		ClassReader reader = null;
		try {
			reader = new ClassReader(stream);
		} catch (IOException e) { return null; }

		ClassNode cn = new ClassNode();
		reader.accept(cn, 0);	

		return cn;
	}
	
	public static List<MethodNode> getMatchingMethods(ClassNode cn, String name, String desc)
	{
		List<MethodNode> output = new ArrayList<MethodNode>();

		for (MethodNode method : (List<MethodNode>)cn.methods) {
			if ((name == null || (name != null && method.name.equals(name))) &&
					(desc == null || (desc != null && method.desc.equals(desc)))) output.add(method);
		}

		return output;
	}
	
	public static <T> List<T> getAllInsnNodesOfType(AbstractInsnNode startInsn, Class<T> classType)
	{				
		List<T> list = new ArrayList<T>();
		
		for (AbstractInsnNode insn = startInsn; insn != null; insn = insn.getNext()) {
			if (insn.getClass() == classType) list.add((T)insn);
		}

		return list;
	}	
	
	public String getMinecraftClass()
	{
		ClassNode main = getClassNode("net/minecraft/client/main/Main");
		if (main == null) return null;

		List<MethodNode> methods = getMatchingMethods(main, "main", "([Ljava/lang/String;)V");
		if (methods.size() != 1) return null;
		MethodNode mainMethod = methods.get(0);

		String minecraftClassName = null;
		String gameConfigClassName = null;
		boolean confirmed = false;

		// We're looking for these instructions:
		// NEW net/minecraft/client/Minecraft
		// INVOKESPECIAL net/minecraft/client/Minecraft.<init> (Lnet/minecraft/client/main/GameConfiguration;)V
		// INVOKEVIRTUAL net/minecraft/client/Minecraft.run ()V
		for (AbstractInsnNode insn = mainMethod.instructions.getLast(); insn != null; insn = insn.getPrevious())
		{
			if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
				MethodInsnNode mn = (MethodInsnNode)insn;
				minecraftClassName = mn.owner;
			}

			else if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
				MethodInsnNode mn = (MethodInsnNode)insn;

				// Check for something wrong
				if (minecraftClassName == null || !mn.owner.equals(minecraftClassName)) return null;

				Type t = Type.getMethodType(mn.desc);
				Type[] args = t.getArgumentTypes();
				if (args.length != 1) return null;

				// Get this while we're here
				gameConfigClassName = args[0].getClassName();
			}

			else if (insn.getOpcode() == Opcodes.NEW) {
				TypeInsnNode vn = (TypeInsnNode)insn;
				if (minecraftClassName != null && vn.desc.equals(minecraftClassName)) {
					confirmed = true;
					break;
				}
			}
		}

		return minecraftClassName;		
	}
	
}
