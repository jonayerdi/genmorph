package ch.usi.gassert.mrip;

import ch.usi.gassert.util.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class MRIPGroup {

    public MRIP[] mrips;
    public Set<String> coverage;

    public MRIPGroup(final MRIP[] mrips) {
        this.mrips = mrips;
        this.coverage = getCoverage(Arrays.stream(mrips));
    }

    public MRIPGroup replaceMRIP(final MRIP newMrip, int position) {
        final MRIP[] newMrips = new MRIP[this.mrips.length];
        for (int i = 0 ; i < newMrips.length ; ++i) {
            if (i == position) {
                newMrips[i] = newMrip;
            } else {
                newMrips[i] = this.mrips[i];
            }
        }
        return new MRIPGroup(newMrips);
    }

    public MRIPGroup[] replaceMRIP(final MRIP newMrip) {
        final MRIPGroup[] newMripGroups = new MRIPGroup[this.mrips.length];
        for (int i = 0 ; i < newMripGroups.length ; ++i) {
            newMripGroups[i] = replaceMRIP(newMrip, i);
        }
        return newMripGroups;
    }

    public static Set<String> getCoverage(final Stream<MRIP> mrips) {
        return mrips
            .map(mrip -> mrip.coveredTestCases)
            .reduce(new HashSet<>(64), Utils::unionWith);
    }

}
