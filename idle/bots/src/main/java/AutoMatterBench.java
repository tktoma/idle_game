import idle.core.Game;
import idle.core.GameState;
import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.save.SaveCodec;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

/**
 * Expérience A/B reproductible à checkpoint, politique neutre : sans bots qui sélectionnent
 * eux-mêmes des substances. La seule différence est l'option de suivi automatique de matière.
 * Usage: AutoMatterBench checkpoint.save heures pas_secondes graine false|true sortie.csv
 */
public final class AutoMatterBench {
    private static long currentMatter(Game g) {
        long sum=0;
        for(Molecule.State state:Molecule.State.values()) sum += g.gatheredInState(state);
        return sum;
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=6) throw new IllegalArgumentException("checkpoint hours dt seed followMatter output.csv");
        Path save=Path.of(args[0]); double hours=Double.parseDouble(args[1]);
        double dt=Double.parseDouble(args[2]); long seed=Long.parseLong(args[3]);
        boolean enabled=Boolean.parseBoolean(args[4]);Path out=Path.of(args[5]);
        GameState state=SaveCodec.read(Files.readString(save)).state();
        Game g=new Game(state,Upgrades.DEFAULT,Automations.DEFAULT,DarkUpgrades.DEFAULT,
                Achievements.DEFAULT,new Random(seed));
        if(!g.isMoleculeAutomationUnlocked() || !g.isStatesUnlocked())
            throw new IllegalStateException("checkpoint trop précoce : besoin de création automatique et des états");
        // Politique égale dans les deux branches : le suivi des recettes et les amas manuels sont
        // éteints afin de mesurer SEULEMENT le gain du nouveau suivi des besoins de matière.
        g.setAutoFollowList(false);
        for(String id:java.util.List.copyOf(state.automatedMolecules())) g.setMoleculeAutomated(id,false);
        g.setMoleculeAutomationEnabled(true);
        g.setAutoFollowMatter(enabled);
        if(g.shoppingList().matter().isEmpty())
            System.err.println("AVERTISSEMENT : cette sauvegarde n'a pas de besoin de matière à l'instant zéro.");
        StringBuilder csv=new StringBuilder("hours,mode,bb,molecules,gathered,space,cosmos,shopping,missing_states\n");
        long steps=(long)Math.ceil(hours*3600.0/dt);
        for(long i=0;i<=steps;i++) {
            if(i>0)g.tick(Math.min(dt,hours*3600-(i-1)*dt));
            if(i==0 || i%Math.max(1,(int)Math.ceil(3600.0/dt))==0 || i==steps){
                csv.append(String.format(Locale.ROOT,"%.5f,%s,%d,%d,%d,%s,%d,%s,%d%n",
                        Math.min(hours,i*dt/3600),enabled?"on":"off",g.bigBangs(),g.moleculesCreated(),
                        currentMatter(g),state.space(),RobotCosmosPlanner.cosmosStage(g),
                        g.shoppingList().target().replace(',',';'),g.shoppingList().matter().size()));
            }
        }
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out,csv);
        System.out.printf(Locale.ROOT,"AB_RESULT,%s,molecules=%d,matter=%d,cosmos=%d,output=%s%n",
                enabled?"on":"off",g.moleculesCreated(),currentMatter(g),
                RobotCosmosPlanner.cosmosStage(g),out);
    }
}
