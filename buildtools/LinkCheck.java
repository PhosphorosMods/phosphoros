import org.objectweb.asm.*; import java.util.*; import java.util.zip.*; import java.io.*; import java.nio.file.*;
// args: classesdir excludePrefix(or -) jar...   checks member refs of classes (excluding those starting with excludePrefix) resolve
public class LinkCheck {
  static Map<String,String[]> supers=new HashMap<>(); static Set<String> mem=new HashSet<>();
  static void index(byte[] b){ ClassReader cr=new ClassReader(b); final String cn=cr.getClassName(); String[] it=cr.getInterfaces(); String[] s=new String[it.length+1]; s[0]=cr.getSuperName(); System.arraycopy(it,0,s,1,it.length); supers.put(cn,s);
    cr.accept(new ClassVisitor(Opcodes.ASM5){ public MethodVisitor visitMethod(int ac,String n,String d,String sg,String[] ex){ mem.add(cn+"."+n+d); return null;}
      public FieldVisitor visitField(int ac,String n,String d,String sg,Object v){ mem.add(cn+"."+n+":"+d); return null;}},ClassReader.SKIP_CODE); }
  static boolean has(String o,String k){ if(o==null)return false; if(o.startsWith("java/")||o.startsWith("javax/")||o.startsWith("sun/"))return true; if(o.startsWith("["))return true; if(mem.contains(o+"."+k))return true; String[] s=supers.get(o); if(s==null){ return false;} for(String x:s) if(has(x,k))return true; return false; }
  public static void main(String[] a) throws Exception {
    for (int i=2;i<a.length;i++){ ZipFile z=new ZipFile(a[i]); for(Enumeration<? extends ZipEntry> e=z.entries();e.hasMoreElements();){ZipEntry ze=e.nextElement(); if(ze.getName().endsWith(".class")&&!ze.getName().startsWith("META-INF")) try{ index(McpRemap.readAll(z.getInputStream(ze))); }catch(IllegalArgumentException ex){}} }
    final List<Path> cls=new ArrayList<>(); Files.walk(Paths.get(a[0])).filter(p->p.toString().endsWith(".class")).forEach(cls::add);
    for(Path p:cls) index(Files.readAllBytes(p));
    final String ex=a[1]; final int[] bad={0}; final Set<String> missingCls=new TreeSet<>();
    for(Path p:cls){ ClassReader cr=new ClassReader(Files.readAllBytes(p)); final String me=cr.getClassName(); if(!ex.equals("-")&&me.startsWith(ex))continue;
      cr.accept(new ClassVisitor(Opcodes.ASM5){ public MethodVisitor visitMethod(int ac,final String mn,String d,String sg,String[] exs){ return new MethodVisitor(Opcodes.ASM5){
        void chk(String o,String k){ if(!has(o,k)){ if(!supers.containsKey(o)&&!o.startsWith("java")) missingCls.add(o); System.out.println("UNRESOLVED in "+me+"."+mn+": "+o+"."+k); bad[0]++; } }
        public void visitMethodInsn(int op,String o,String n,String d,boolean itf){ chk(o,n+d);} public void visitFieldInsn(int op,String o,String n,String d){ chk(o,n+":"+d);} 
        public void visitTypeInsn(int op,String t){ if(!t.startsWith("[")&&!t.startsWith("java")&&!supers.containsKey(t)){missingCls.add(t);System.out.println("MISSING CLASS in "+me+": "+t);bad[0]++;}} };}},0); }
    System.out.println("unresolved refs: "+bad[0]+" missing classes: "+missingCls);
  }
}
