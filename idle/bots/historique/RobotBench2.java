import idle.core.*;import idle.core.upgrades.*;import java.util.*;
public class RobotBench2{
 static class Result{String name;double first=-1,auto=-1;int fusions,buys;Result(String n){name=n;}}
 static void purchase(Game g,int mode,Result r){
  if(mode==0){for(Upgrade u:g.upgrades(Resource.PARTICLES)){int n=0;while(g.buy(u.id())&&n++<10000)r.buys++;}}
  else if(mode==1){for(int k=0;k<10000;k++){Upgrade best=null;for(Upgrade u:g.upgrades(Resource.PARTICLES))if(g.canBuy(u.id())&&(best==null||g.costOf(u.id()).lt(g.costOf(best.id()))))best=u;if(best==null)break;g.buy(best.id());r.buys++;}}
  else if(mode==2){for(Upgrade u:g.upgrades(Resource.PARTICLES))if(g.buy(u.id()))r.buys++;}
  else{for(int k=0;k<10000;k++){Upgrade best=null;for(Upgrade u:g.upgrades(Resource.PARTICLES))if(g.canBuy(u.id())&&(best==null||g.costOf(u.id()).gt(g.costOf(best.id()))))best=u;if(best==null)break;g.buy(best.id());r.buys++;}}
 }
 static Result run(String name,int mode,double reaction,double cap){Game g=new Game();g.start();Result r=new Result(name);double t=0,next=0;int guard=0;while(t<cap&&!g.isAutomationUnlocked()){double step=Math.min(0.25,Math.max(.00001,next-t));g.tick(step);t+=step;if(t+1e-7>=next){purchase(g,mode,r);next+=reaction;while(g.canFuse()){if(r.first<0)r.first=t;g.fuse();r.fusions++;for(int i=0;i<500;i++){Upgrade cheapest=null;for(Upgrade u:g.upgrades(Resource.ATOMS)){if(u.id().equals("atom_keep")||!g.canBuy(u.id()))continue;if(cheapest==null||g.costOf(u.id()).lt(g.costOf(cheapest.id())))cheapest=u;}if(cheapest==null)break;g.buy(cheapest.id());}purchase(g,mode,r);}}if(++guard>3000000)break;}if(g.isAutomationUnlocked())r.auto=t;System.out.printf(Locale.US,"%s,%.2f,%.2f,%d,%d,%.2f,%d%n",name,r.first/60,r.auto/60,r.fusions,r.buys,t/60,guard);return r;}
 public static void main(String[] args){System.out.println("profil,premiere_fusion_min,automation_min,fusions,achats,temps_min,steps");run("optimiseur_0.5s",1,.5,7200);run("acheteur_catalogue_0.5s",0,.5,7200);run("joueur_reflechi_5s",1,5,7200);run("occasionnel_30s",2,30,36000);run("mauvais_choix_5s",3,5,7200);}
}
