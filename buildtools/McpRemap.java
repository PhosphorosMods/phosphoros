import org.objectweb.asm.*; import org.objectweb.asm.commons.*; import java.util.*; import java.util.zip.*; import java.io.*;
public class McpRemap {
  public static void main(String[] a) throws Exception {
    final Map<String,String> m=new HashMap<>();
    for (String f: new String[]{"fields.csv","methods.csv"}) { BufferedReader r=new BufferedReader(new FileReader(a[0]+"/"+f)); String l; r.readLine(); while((l=r.readLine())!=null){ String[] p=l.split(",",-1); m.put(p[0],p[1]);} }
    Remapper rm=new Remapper(){ String g(String n){String v=m.get(n);return v==null?n:v;}
      public String mapMethodName(String o,String n,String d){return g(n);} public String mapFieldName(String o,String n,String d){return g(n);} public String mapInvokeDynamicMethodName(String n,String d){return g(n);} };
    ZipFile in=new ZipFile(a[1]); ZipOutputStream out=new ZipOutputStream(new FileOutputStream(a[2]));
    for (Enumeration<? extends ZipEntry> e=in.entries(); e.hasMoreElements();){ ZipEntry ze=e.nextElement(); if(ze.isDirectory()||ze.getName().startsWith("META-INF"))continue;
      byte[] b=readAll(in.getInputStream(ze));
      if(ze.getName().endsWith(".class")){ ClassReader cr=new ClassReader(b); ClassWriter cw=new ClassWriter(0); cr.accept(new ClassRemapper(cw,rm),0); b=cw.toByteArray(); }
      out.putNextEntry(new ZipEntry(ze.getName())); out.write(b); out.closeEntry(); }
    out.close();
  }
  static byte[] readAll(InputStream i) throws IOException { ByteArrayOutputStream o=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; while((n=i.read(buf))>0)o.write(buf,0,n); return o.toByteArray(); }
}
