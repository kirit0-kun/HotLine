package com.flowapp.HotLine;

import com.flowapp.HotLine.Models.HotTableRow;
import com.flowapp.HotLine.Utils.Constants;
import com.flowapp.HotLine.Utils.FileUtils;
import de.vandermeer.asciitable.AsciiTable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RefiningProject {

    public static void main(String[] args) {
        FileUtils.clear();
        final Float iDmm = 307f;
        final Float oDmm = 323.6f;
        final float spGr = 0.92f;
        final float visAt100F = 460f;
        final float visAt212F = 120f;
        final float flowRateM3H = 400f;
        final Float maxTempC = 84f;
        final Float minTempC = 56f;
        final float tsC = 20f;
        final float lambdaS = 2f;
        final float tinIn = 1.5f;
        final float h = 1.5f;
        final float lambdaC = 0.02f;
        final float alphaT = 0.65f;
        final float tf1 = 76f;
        final float tf2 = 72f;
        final float maxPumpPressure = 90f;
        final Float maxTotalPressure = null;
        final boolean reverse = false;

        if (maxTotalPressure == null && (minTempC == null || maxTempC == null)) {
            throw new IllegalArgumentException("Must either provide a temperature boundaries or a maximum pumping pressure");
        }

        final float iD = iDmm / Constants.MmInMeter;
        final float oD = oDmm / Constants.MmInMeter;

        final float dIn = oD + 2 * tinIn * Constants.MmInInch / Constants.MmInMeter;
        println("din = OD + 2 tin = {} + 2 * {} * {} = {} m", oD, tinIn, Constants.MmInInch / Constants.MmInMeter, dIn);
        final float tfBar = (tf1 + tf2) / 2;
        final float velocityMSec = (float) (4 * flowRateM3H / (3.14 * 3600 * Math.pow(iD, 2)));
        println ("v = 4Q/(π * ID^2) = $ * {} / (π * ({})^2) = {} m/sec", flowRateM3H, iD, velocityMSec);
        println("Tf  = ({} + {}) / 2 = {} C", tf1, tf2, tfBar);
        float deltaT = 1.5f;
        float k;
        while (true)
        {
            println("Assume ∆T = {} C", deltaT);
            final float[] result = calculatePhysicalProperties(iD, oD, spGr, velocityMSec, tsC, lambdaS, h, alphaT, visAt100F,
                                                            visAt212F, deltaT, lambdaC, dIn, tfBar);
            final float newTi = result[0];
            k = result[1];
            final float newDeltaT = tfBar - newTi;
            println("then ∆T = {} C", newDeltaT);
            final float diff = Math.abs(newDeltaT - deltaT);
            if (diff > Constants.AllowedErrorInDeltaT) {
                println("| assumed ∆T - calculated ∆T | = | {} - {} | = {} C > {} C, so unsuitable", deltaT, newDeltaT, diff, Constants.AllowedErrorInDeltaT);
            } else {
                println("| assumed ∆T - calculated ∆T | = | {} - {} | = {} C < {} C, so suitable", deltaT, newDeltaT, diff, Constants.AllowedErrorInDeltaT);
                break;
            }
            deltaT = newDeltaT;
        }
        final float sectionDiff = Math.abs(tf1 - tf2);
        Integer noSections = null;
        if (minTempC != null && maxTempC != null) {
            noSections = (int) Math.ceil((maxTempC - minTempC) / sectionDiff);
            println("No. of Sections = ({} + {}) / {} = {} sections", maxTempC, minTempC, sectionDiff, noSections);
        }
        final float start;
        final float delta;
        if (reverse) {
            start = minTempC;
            delta = sectionDiff;
        } else {
            start = maxTempC;
            delta = -sectionDiff;
        }
        final Float end;
        if (noSections != null) {
            end = start + delta * noSections;
        } else {
            end = null;
        }
        float totalLength = 0;
        float totalPressure = 0;
        final List<HotTableRow> hotTableRows = new ArrayList<>();
        float first = start;
        while (maxTotalPressure == null ? (reverse ? first < end : end < first) : totalPressure < maxTotalPressure) {
            final float t1 = first;
            final float t2 = first + delta;
            final var section = calculateHotTable(t2, t1, alphaT, tsC, spGr, deltaT, flowRateM3H, k, velocityMSec, iD, visAt100F, visAt212F, totalLength, totalPressure);
            hotTableRows.add(section);
            totalLength += section.getL();
            totalPressure += section.getDeltaP();
            first += delta;
        }
        renderHotTable(hotTableRows);
        final int noStations = (int) Math.ceil(totalPressure / maxPumpPressure);
        println("No. of Stations = {}/{} = {} stations", totalPressure, maxPumpPressure, noStations);
    }

    private static void renderHotTable(List<HotTableRow> hotTableRows) {
        final List<Object[]> table = new ArrayList<>();
        table.add(new Object[]{"No.", "Tf1", "Tf2", "Tf", "Ti", "ζi", "C", "Pt", "L", "ΣL", "Nre", "F", "hf", "∆P", "Σ∆P"});
        for (int i = 0; i < hotTableRows.size(); i++) {
            final var section = hotTableRows.get(i);
            table.add(new Object[]{i+1, section.getTf1(), section.getTf2(), section.getTfBar(), section.getTi(), section.getVisAtI(),
                    section.getC(), section.getPt(), section.getL(), section.getSumL(), section.getNre(),
                    section.getF(), section.getHf(), section.getDeltaP(), section.getSumP()});
        }
        renderTable(table);
    }

    @NotNull
    private static HotTableRow calculateHotTable(float tf2,
                                                 float tf1,
                                                 float alphaT,
                                                 float tsC,
                                                 float spGr,
                                                 float deltaT,
                                                 float flowRateM3H,
                                                 float k,
                                                 float velocityMSec,
                                                 float iD,
                                                 float visAt100F,
                                                 float visAt212F,
                                                 float totalLength,
                                                 float totalPressure) {

        final float j = (float) (Math.log10(Math.log10(visAt100F)) - Math.log10(Math.log10(visAt212F))) / 64;
        final float g = (float) (Math.log10(Math.log10(visAt100F)) + 36 * j);
        final float tfBar = (tf1 + tf2) / 2;
        final float tI = tfBar - deltaT;
        final float visAtI = (float) Math.pow(10, Math.pow(10, g - j * tI));
        final float pT = 1000 * spGr - alphaT * (tI - 20);
        final float c = (float) ((762.3 + 3.38 * (tI+Constants.ZeroCInKelvin)) / Math.sqrt(spGr));
        final float length = (float) (- flowRateM3H * spGr * c * Math.log((tf2 - tsC) / (tf1 - tsC)) / (3600 * k));
        final float sumLength = totalLength + length;
        final float nRe = velocityMSec * iD * 1e+6f / visAtI;
        final float f;
        if (nRe <= 2100) {
            f = 64 / nRe;
        } else if (nRe < 4000) {
            f = (float) (0.5 / Math.pow(nRe, 0.3));
        } else {
            f = (float) (0.316 / Math.pow(nRe, 0.25));
        }
        final float h = (float) (1000 * f * length * Math.pow(velocityMSec, 2) / (19.6f * iD));
        final float p = h * pT / 10000;
        final float sumPressure = totalPressure + p;
        return new HotTableRow(tf1, tf2, tfBar, tI, visAtI, c, pT, length, sumLength, nRe, f, h, p, sumPressure);
    }

    private static float[] calculatePhysicalProperties(float iD,
                                                     float oD,
                                                     float spGr,
                                                     float v,
                                                     float tsC,
                                                     float lambdaS,
                                                     float h,
                                                     float alphaT,
                                                     float visAt100F,
                                                     float visAt212F,
                                                     float assumedDt,
                                                     float lambdaC,
                                                     float dIn,
                                                     float tfBar) {
        
        float ti = tfBar - assumedDt;
        println("Ti = Tf - ∆T = {} - {} = {} C", tfBar, assumedDt, ti);

        println("Physical Properties:- ");
        double shortLog = Math.log(Math.log(visAt100F) / Math.log(10)) / Math.log(10);
        final float b = (float) ((shortLog - Math.log(Math.log(visAt212F) / Math.log(10)) / Math.log(10)) / 62);
        final float a = (float) (shortLog + 36 * b);
        final float mi = (float) Math.pow( 10, Math.pow(10.0, (a - b * ti)));
        println ("kin.v i = {}", mi);

        final float mlam = (v * iD * 1e+6f) / 2100;
        final float mtur = (v * iD * 1e+6f) / 4000;
        println ("kin.v lam = {}", mlam);
        println ("kin.v tur = {}", mtur);
        final float nre = (v * iD) / (mi * 1e-6f);
        println ("Nre = {}", nre);
        final float c = (float) ((762.5f + 3.38f * (ti + Constants.ZeroCInKelvin)) / Math.pow(spGr, 0.5));
        println ("C = {}", c);
        final float bt = (float) (1 / (2583 - 6340 * spGr + 5965 * Math.pow(spGr, 2) - (ti + 273)));
        println ("Bt = {}", String.format("%.7f", bt));
        final float pf = (0.134f - 6.31f * 1e-5f * (ti + 273)) / spGr;
        println ("λf = {}", pf);
        final float pt = 1000 * spGr - alphaT * (ti - 20);
        println ("pt = {}", pt);
        final float nGrPr = (float) ((Math.pow(iD, 3) * c * pt * bt * 9.8 * (tfBar - ti)) / (mi * 1e-6f * pf));
        println ("NGR,PR = {}", (int) nGrPr);
        final float nu;
        if (nre <= 2100) {
            if (nGrPr >50000) {
                nu = (float) (0.184 * Math.pow(nGrPr,0.32));
            } else {
                nu = 3.8f;
            }
        } else if (nre < 4000) {
            nu = (float) (0.027 * Math.pow(nre,0.8) * Math.pow(c * pt * (mi * 1e-6f) / pf, 1 / 3.0) * (1 - (6e+5 / Math.pow(nre,1.8))));
        } else {
            nu = (float) (0.027 * Math.pow(nre, 0.8) * Math.pow(c * pt * mi * 1e-6f / pf,1 / 3.0));
        }
        println ("Nu = {}", nu);
        final float alpha1 = nu * pf / iD;
        println ("α1 = {}", alpha1);
        final float alpha2 = (float) (2 * lambdaS / (dIn * Math.log(4 * h / dIn)));
        println ("α2 = {}", alpha2);
        final float k = (float) (Math.PI / (1/(alpha1 * iD) + Math.log(dIn / oD) / (2 * lambdaC) + 1/(dIn * alpha2)));
        println("K = {}",k);
        ti = (float) (tfBar - (k * (tfBar - tsC) / (Math.PI * alpha1 * iD)));
        println("Ti(calc) = {} C", ti);
        return new float[]{ti, k};
    }

    private static void renderTable(List<Object[]> args) {
        renderTable(args.toArray(new Object[0][0]));
    }
    private static void renderTable(Object[] ... args) {
        AsciiTable at = new AsciiTable();
        at.addRule();
        for (var row: args) {
            at.addRow(row);
            at.addRule();
        }
        String rend = at.render();
        println(rend);
    }

    private static void println(@NotNull String pattern, Object... args) {
        final String message = format(pattern, args);
        System.out.println(message);
        FileUtils.printOut(message);
    }

    @NotNull
    private static String format(@NotNull String pattern, Object... args) {
        Pattern rePattern = Pattern.compile("\\{([0-9+-]*)}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = rePattern.matcher(pattern);
        int counter = -1;
        while (matcher.find()) {
            counter++;
            String number = matcher.group(1);
            if (number == null) {
                number = "";
            }
            if (!number.isBlank()) {
                if (number.equals("+")) {
                    number = "\\+";
                    counter++;
                } else if (number.equals("-")) {
                    counter--;
                } else {
                    counter = Integer.parseInt(number);
                }
            }
            counter = clamp(counter, 0, args.length - 1);
            String toChange = "\\{" + number + "}";
            String result = args[counter].toString();
            pattern = pattern.replaceFirst(toChange, result);
        }
        return pattern;
    }

    private static <T extends Comparable<T>> T clamp(T val, T min, T max) {
        if (val.compareTo(min) < 0) return min;
        else if (val.compareTo(max) > 0) return max;
        else return val;
    }
}
