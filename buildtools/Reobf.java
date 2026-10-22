import org.objectweb.asm.*; import org.objectweb.asm.commons.*; import java.util.*; import java.util.zip.*; import java.io.*; import java.nio.file.*;
// Reobf: args: csvdir outdir classesdir srgjar1 [srgjar2 ...]
public class Reobf {
  static Map<String,String> csv=new HashMap<>();
  static Map<String,String[]> supers=new HashMap<>(); // class -> [super, ifaces...]
  static Map<String,String> meth=new HashMap<>(), fld=new HashMap<>(); // owner.mcp.desc -> srg ; owner.mcp -> srg
  static void index(byte[] b){ ClassReader cr=new ClassReader(b); final String cn=cr.getClassName(); String[] it=cr.getInterfaces(); String[] s=new String[it.length+1]; s[0]=cr.getSuperName(); System.arraycopy(it,0,s,1,it.length); supers.put(cn,s);
    cr.accept(new ClassVisitor(Opcodes.ASM5){ public MethodVisitor visitMethod(int ac,String n,String d,String sg,String[] ex){ if(csv.containsKey(n)) meth.put(cn+"."+csv.get(n)+d,n); return null;}
      public FieldVisitor visitField(int ac,String n,String d,String sg,Object v){ if(csv.containsKey(n)) fld.put(cn+"."+csv.get(n),n); return null;}},ClassReader.SKIP_CODE); }
  static String findM(String o,String n,String d){ if(o==null)return null; String v=meth.get(o+"."+n+d); if(v!=null)return v; String[] s=supers.get(o); if(s==null)return null; for(String x:s){v=findM(x,n,d); if(v!=null)return v;} return null; }
  static String findF(String o,String n){ if(o==null)return null; String v=fld.get(o+"."+n); if(v!=null)return v; String[] s=supers.get(o); if(s==null)return null; for(String x:s){v=findF(x,n); if(v!=null)return v;} return null; }
  public static void main(String[] a) throws Exception {
    for (String f: new String[]{"fields.csv","methods.csv"}) { BufferedReader r=new BufferedReader(new FileReader(a[0]+"/"+f)); String l; r.readLine(); while((l=r.readLine())!=null){ String[] p=l.split(",",-1); csv.put(p[0],p[1]);} }
    for (int i=3;i<a.length;i++){ ZipFile z=new ZipFile(a[i]); for(Enumeration<? extends ZipEntry> e=z.entries();e.hasMoreElements();){ZipEntry ze=e.nextElement(); if(ze.getName().endsWith(".class")) index(McpRemap.readAll(z.getInputStream(ze)));} }
    final List<Path> cls=new ArrayList<>(); Files.walk(Paths.get(a[2])).filter(p->p.toString().endsWith(".class")).forEach(cls::add);
    for(Path p:cls){ ClassReader cr=new ClassReader(Files.readAllBytes(p)); String[] it=cr.getInterfaces(); String[] s=new String[it.length+1]; s[0]=cr.getSuperName(); System.arraycopy(it,0,s,1,it.length); supers.put(cr.getClassName(),s); }
    Remapper rm=new Remapper(){ public String mapMethodName(String o,String n,String d){ if(n.startsWith("<"))return n; String v=findM(o,n,d); return v==null?n:v; }
      public String mapFieldName(String o,String n,String d){ String v=findF(o,n); return v==null?n:v; } };
    int c=0; for(Path p:cls){ ClassReader cr=new ClassReader(Files.readAllBytes(p)); ClassWriter cw=new ClassWriter(0); cr.accept(new ClassRemapper(cw,rm),0); Path o=Paths.get(a[1]).resolve(Paths.get(a[2]).relativize(p)); Files.createDirectories(o.getParent()); Files.write(o,cw.toByteArray()); c++; }
    System.out.println("reobf classes: "+c);
  }
}
