import idle.core.*;import idle.core.upgrades.*;import java.util.*;
public class RobotPhase2 {
 static class R {String name;double first=-1,auto=-1,firstElement=-1,firstExplosion=-1;int fusions=0,syntheses=0,buys=0;R(String n){name=n;}}
 static void buyParticles(Game g,int mode,R r){
  if(mode==2){for(Upgrade u:g.upgrades(Resource.PARTICLES))if(g.buy(u.id()))r.buys++;return;}
  for(int i=0;i<3000;i++){Upgrade b=null;for(Upgrade u:g.upgrades(Resource.PARTICLES))if(g.canBuy(u.id())&&(b==null||(mode==1?g.costOf(u.id()).gt(g.costOf(b.id())):g.costOf(u.id()).lt(g.costOf(b.id())))))b=u;if(b==null)break;g.buy(b.id());r.buys++;}
 }
 static void atomBuys(Game g,int mode){for(int i=0;i<300;i++){Upgrade b=null;for(Upgrade u:g.upgrades(Resource.ATOMS)){if(u.id().equals("atom_keep")||!g.canBuy(u.id()))continue;if(b==null||(mode==1?g.costOf(u.id()).gt(g.costOf(b.id())):g.costOf(u.id()).lt(g.costOf(b.id()))))b=u;}if(b==null)break;g.buy(b.id());}}
 static void act(Game g,int mode,R r,double t){buyParticles(g,mode,r);for(int i=0;i<5&&g.canFuse();i++){if(r.first<0)r.first=t;g.fuse();r.fusions++;atomBuys(g,mode);buyParticles(g,mode,r);}if(g.isAutomationUnlocked()&&r.auto<0)r.auto=t;
  // Favor purchases of automation before upgrades to prevent starving automation.
  for(Automation a:g.automations())if(g.canBuyAutomation(a.id()))g.buyAutomation(a.id());
  for(Automation a:g.automations())if(g.canSpeedUpAutomation(a.id()))g.speedUpAutomation(a.id());
  atomBuys(g,mode);
  for(int i=0;i<15&&g.canSynthesize();i++){g.synthesize();r.syntheses++;if(r.firstElement<0&&g.discoveredElements()>0)r.firstElement=t;}
  if(g.canExplode()){r.firstExplosion=t;}
 }
 static void run(String name,int mode,double interval,double hours){Game g=new Game();g.start();R r=new R(name);double t=0,step=Math.min(interval,1);long ticks=0;long max=(long)(hours*3600/step);while(t<hours*3600&&r.firstExplosion<0){g.tick(step);t+=step;if(++ticks%Math.max(1,(long)Math.round(interval/step))==0)act(g,mode,r,t);}
 System.out.printf(Locale.US,"%s,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%.2f,%d,%s%n",name,r.first/60,r.auto/60,r.firstElement/60,r.firstExplosion/60,r.fusions,r.syntheses,g.discoveredElements(),t/60,r.buys,g.state().atoms());}
 public static void main(String[] a){System.out.println("profil,fusion_min,automation_min,premier_element_min,explosion_min,fusions,syntheses,elements,temps_min,achats,atomes");run("actif_1s",0,1,5);run("maladroit_5s",1,5,5);run("occasionnel_30s",2,30,5);}
}
