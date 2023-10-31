package ch.usi.gassert;

import ch.usi.gassert.data.manager.IDataManager;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.FileUtils;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.List;

public class BestIndividuals {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.00000");

    protected final PrintStream out;
    protected final IDataManager dataManager;
    protected final int count;

    public BestIndividuals(final String outFile, final IDataManager dataManager, final int count) throws FileNotFoundException, UnsupportedEncodingException {
        FileUtils.mkdirsFile(outFile);
        this.out = new PrintStream(outFile, "UTF-8");
        this.dataManager = dataManager;
        this.count = count;
    }

    public void writeIndividuals(int generation, List<Individual> individuals) {
        this.out.println("[GENERATION " + generation + "]");
        individuals.stream().limit(count).forEach(individual -> {
            final TreeGroup tg = individual.getTreeGroup();
            this.out.println(new Tree(
                    "=>",
                    this.dataManager.getInputRelation(tg).asTree(),
                    this.dataManager.getOutputRelation(tg).asTree(),
                    Tree.Type.BOOLEAN
            ));
            this.out.println(DECIMAL_FORMAT.format(individual.getFitnessValueFP()) + "," + DECIMAL_FORMAT.format(individual.getFitnessValueFN()));
        });
    }

    public void close() {
        this.out.close();
    }

}
