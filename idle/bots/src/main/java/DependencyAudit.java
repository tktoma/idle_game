import idle.core.*;import idle.core.upgrades.*;import idle.core.darkmatter.*;import idle.core.molecules.*;import idle.core.sky.*;import java.util.*;import java.nio.file.*;import java.io.*;
/** Exporte le graphe de prerequis reels des molecules, assemblages et astres. */
public class DependencyAudit {
 public static void main(String[]args)throws Exception{
  Path out=Path.of(args[0]);Files.createDirectories(out);Game g=new Game(new GameState(),Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,idle.core.achievements.Achievements.DEFAULT,new Random(42));
  try(PrintWriter e=new PrintWriter(Files.newBufferedWriter(out.resolve("dependances.csv")));PrintWriter n=new PrintWriter(Files.newBufferedWriter(out.resolve("objets_catalogue.csv")))){
   e.println("source_type,source_id,cible_type,cible_id,quantite,contrainte");n.println("type,id,nom,categorie");
   for(Molecule m:g.molecules())n.printf("MOLECULE,%s,%s,%s%n",m.id(),m.name().replace(',',';'),m.kind());
   for(Assembly a:g.assemblies()){
    n.printf("ASSEMBLAGE,%s,%s,%s%n",a.id(),a.name().replace(',',';'),a.family());
    for(var ingr:a.ingredients().entrySet())e.printf("MOLECULE,%s,ASSEMBLAGE,%s,%d,rassemblee%n",ingr.getKey(),a.id(),ingr.getValue());
   }
   for(Body b:g.bodies()){
    n.printf("ASTRE,%s,%s,%s%n",b.id(),b.name().replace(',',';'),b.tier());
    for(String prereq:b.bodies())e.printf("ASTRE,%s,ASTRE,%s,1,mature%n",prereq,b.id());
    for(String asm:b.assemblies())e.printf("ASSEMBLAGE,%s,ASTRE,%s,1,forme%n",asm,b.id());
    for(var m:b.molecules().entrySet())e.printf("MOLECULE,%s,ASTRE,%s,%d,possede%n",m.getKey(),b.id(),m.getValue());
    for(var s:b.matter().entrySet())e.printf("MATIERE,%s,ASTRE,%s,%d,rassemblee%n",s.getKey(),b.id(),s.getValue());
   }
   for(Cosmos c:Cosmos.values())n.printf("COSMOS,%s,%s,fin%n",c.name(),c.phrase().replace(',',';'));
  }
  System.out.printf("CATALOGUE,molecules=%d,assemblages=%d,astres=%d,cosmos=%d%n",g.molecules().size(),g.assemblies().size(),g.bodies().size(),Cosmos.values().length);
 }
}
