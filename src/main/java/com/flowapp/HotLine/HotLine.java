package com.flowapp.HotLine;

import com.flowapp.HotLine.Models.*;
import com.flowapp.HotLine.Models.Curves.Curve;
import com.flowapp.HotLine.Models.Curves.Linear;
import com.flowapp.HotLine.Utils.Constants;
import com.flowapp.HotLine.Utils.FileUtils;
import com.flowapp.HotLine.Utils.StreamUtils;
import com.flowapp.HotLine.Utils.TableList;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HotLine {

    private StringBuilder steps;
    
    public HotLineResult hotLine(Float iDmm,
                                 Float oDmm,
                                 float spGr,
                                 float visAt100F,
                                 float visAt212F,
                                 float flowRateM3H,
                                 Float maxTempC,
                                 Float minTempC,
                                 float tsC,
                                 float lambdaS,
                                 float tinIn,
                                 float h,
                                 float lambdaC,
                                 float alphaT,
                                 float tf1,
                                 float tf2,
                                 float dtAssumption,
                                 float dTAllowedError,
                                 float maxPumpPressure,
                                 Float maxTotalPressure,
                                 float pumpInitialIntakePressure,
                                 boolean reverse, boolean simplifiedOnly) {
        clear();
        if (maxTotalPressure == null && (minTempC == null || maxTempC == null)) {
            throw new IllegalArgumentException("Must either provide a temperature boundaries or a maximum pumping pressure");
        }
//        if (reverse && maxTotalPressure == null && maxTempC == null) {
//            throw new IllegalArgumentException("Must either provide a temperature boundaries or a maximum pumping pressure");
//        }
//        if (!reverse && maxTotalPressure == null && maxTempC == null) {
//            throw new IllegalArgumentException("Must either provide a temperature boundaries or a maximum pumping pressure");
//        }

        final float iD = iDmm / Constants.MmInMeter;
        final float oD = oDmm / Constants.MmInMeter;

        final float dIn = oD + 2 * tinIn * Constants.MmInInch / Constants.MmInMeter;
        println("din = OD + 2 tin = {} + 2 * {} * {} = {} m", oD, tinIn, Constants.MmInInch / Constants.MmInMeter, dIn);
        final float tfBar = (tf1 + tf2) / 2;
        final float velocityMSec = (float) (4 * flowRateM3H / (3.14 * 3600 * Math.pow(iD, 2)));
        println ("v = 4Q/(π * ID^2) = 4 * {} / (π * ({})^2) = {} m/sec", flowRateM3H, iD, velocityMSec);
        println("Tf  = ({} + {}) / 2 = {} C", tf1, tf2, tfBar);
        float deltaT = dtAssumption;
        PhysicalProperties physicalProperties;
        while (true)
        {
            println("Assume ∆T = {} C", deltaT);
            physicalProperties = calculatePhysicalProperties(iD, oD, spGr, velocityMSec, tsC, lambdaS, h, alphaT, visAt100F,
                                                            visAt212F, deltaT, lambdaC, dIn, tfBar);
            final float newDeltaT = physicalProperties.getDeltaT();
            println("then ∆T = {} C", physicalProperties.getDeltaT());
            final float diff = Math.abs(newDeltaT - deltaT);
            if (diff > dTAllowedError) {
                println("| assumed ∆T - calculated ∆T | = | {} - {} | = {} C > {} C, so unsuitable", deltaT, newDeltaT, diff, dTAllowedError);
                deltaT = newDeltaT;
            } else {
                println("| assumed ∆T - calculated ∆T | = | {} - {} | = {} C < {} C, so suitable", deltaT, newDeltaT, diff, dTAllowedError);
                break;
            }
        }
        physicalProperties.setDeltaT(deltaT);
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
            final float t1 = reverse ? (first + delta) : first;
            final float t2 = reverse ? first : (first + delta);
            final var section = calculateHotTable(t2, t1, alphaT, tsC, spGr, deltaT, flowRateM3H, physicalProperties.getK(), velocityMSec, iD, visAt100F, visAt212F, totalLength, totalPressure);
            hotTableRows.add(section);
            totalLength += section.getL();
            totalPressure += section.getDeltaP();
            first += delta;
        }
        renderHotTable(hotTableRows);
        final int noStations = (int) Math.ceil(totalPressure / maxPumpPressure);
        println("No. of Stations = {}/{} = {} stations", totalPressure, maxPumpPressure, noStations);

        println("Simplified Ford:-");
        final float dtLaminar = (float) (3.39f * Math.pow(tinIn * 2.54f, -3.11));
        final float dtTrans = (float) (2.026f * Math.pow(tinIn * 2.59f, -0.52));
        final float dtTurbulent = (float) (1.08f * Math.pow(tinIn * 2.54f, -0.394));
        println("For Laminar Flow dt = 3.39 * ({} * 2.54) ^ -3.11 = {} C", tinIn, dtLaminar);
        println("For Transitional Flow dt = 2.026 * ({} * 2.59) ^ -0.52 = {} C", tinIn, dtTrans);
        println("For Turbulent Flow dt = 1.08 * ({} * 2.54) ^ -0.394 = {} C", tinIn, dtTurbulent);
        final Map<FlowType, Float> deltaTMap = Map.of(FlowType.LAMINAR, dtLaminar, FlowType.TRANSITIONAL, dtTrans, FlowType.TURBULENT, dtTurbulent);
        final List<HotTableRow> simplifiedFordRows = new ArrayList<>();
        totalLength = 0;
        totalPressure = 0;
        for (var section: hotTableRows) {
            final float dT = deltaTMap.get(section.getFlowType());
            final var newSection = calculateSimplifiedFordHotTable(section.getTf2(), section.getTf1(), alphaT, dT, oD, iD, flowRateM3H, velocityMSec, spGr, tsC, lambdaC, tinIn, physicalProperties.getAlpha2(), visAt100F, visAt212F, totalLength, totalPressure);
            totalLength += newSection.getL();
            totalPressure += newSection.getDeltaP();
            simplifiedFordRows.add(newSection);
        }
        renderSimplifiedFordHotTable(simplifiedFordRows);
        final List<HotTableRow> plotRows;
        if (simplifiedOnly) {
            plotRows = simplifiedFordRows;
        } else {
            plotRows = hotTableRows;
        }
        final var pressureTraverse = calculatePressureTraverse(maxPumpPressure, pumpInitialIntakePressure, reverse, totalLength, plotRows);
        final var temperatureTraverse = calculateTemperatureTraverse(plotRows, reverse);
        return new HotLineResult(physicalProperties, hotTableRows, pressureTraverse, temperatureTraverse, simplifiedFordRows, steps.toString());
    }


//°

    private Point[] calculateTemperatureTraverse(List<HotTableRow> hotTableRows, boolean reverse) {
        final List<Point> pressureTraverse = new ArrayList<>();
        for (var point: hotTableRows) {
            final Point startPoint = Point.of((point.getSumL() - point.getL()) * (reverse ? -1 : 1), reverse ? point.getTf2() : point.getTf1());
            pressureTraverse.add(startPoint);
        }
        final var lastPoint = hotTableRows.get(hotTableRows.size() - 1);
        final Point startPoint = Point.of(lastPoint.getSumL() * (reverse ? -1 : 1), reverse ? lastPoint.getTf1() : lastPoint.getTf2());
        pressureTraverse.add(startPoint);
        if (reverse) {
//            for (int i = 0; i < pressureTraverse.size(); i++) {
//                final var oldPoint = pressureTraverse.get(i);
//                pressureTraverse.set(i, Point.of(oldPoint.getX() - lastPoint.getSumL(), oldPoint.getY()));
//            }
            Collections.reverse(pressureTraverse);
        }
        return pressureTraverse.toArray(new Point[0]);
    }

    @NotNull
    private Point[] calculatePressureTraverse(float maxPumpPressure, float pumpInitialIntakePressure, boolean reverse, float totalLength, List<HotTableRow> hotTableRows) {
        final List<Point> pressureTraverse = new ArrayList<>();
        float finalPressure = reverse ? pumpInitialIntakePressure : maxPumpPressure;
        float finalLength = reverse ? totalLength : 0;
        final int pressureSign = reverse ? 1 : -1;
        final int lengthSign = reverse ? -1 : 1;
        Point lastPoint = Point.of(finalLength, finalPressure);
        pressureTraverse.add(lastPoint);
        for (int i = 0; i < hotTableRows.size(); i++) {
            float lastLength = finalLength;
            float lastPressure = finalPressure;
            final var section = hotTableRows.get(i);
            finalPressure += pressureSign * section.getDeltaP();
            finalLength += lengthSign * section.getL();
            Point newPoint = Point.of(finalLength, finalPressure);
            while (finalPressure > maxPumpPressure) {
                finalPressure = finalPressure - maxPumpPressure + pumpInitialIntakePressure;
                final float middlePressure = maxPumpPressure;
                lastPoint = Point.of(lastLength, lastPressure);
                final Curve line = new Linear(lastPoint, newPoint);
                final float middleLength = line.getX(middlePressure);
                lastLength = middleLength;
                lastPressure = middlePressure;
                lastPoint = Point.of(lastLength, lastPressure);
                pressureTraverse.add(lastPoint);
                newPoint = Point.of(middleLength, pumpInitialIntakePressure);
                pressureTraverse.add(newPoint);
            }
            while (finalPressure < pumpInitialIntakePressure) {
                finalPressure = finalPressure + maxPumpPressure - pumpInitialIntakePressure;
                final float middlePressure = pumpInitialIntakePressure;
                lastPoint = Point.of(lastLength, lastPressure);
                final Curve line = new Linear(lastPoint, newPoint);
                final float middleLength = line.getX(middlePressure);
                lastLength = middleLength;
                lastPressure = middlePressure;
                lastPoint = Point.of(lastLength, lastPressure);
                pressureTraverse.add(lastPoint);
                newPoint = Point.of(middleLength, maxPumpPressure);
                pressureTraverse.add(newPoint);
            }
            newPoint = Point.of(finalLength, finalPressure);
            pressureTraverse.add(newPoint);
        }
        if (!reverse) {
            final Point last = StreamUtils.reverse(pressureTraverse.stream())
                    .filter( item -> item.getY() == maxPumpPressure)
                    .findFirst().orElse(null);
            if (last != null) {
                final int startIndex = pressureTraverse.lastIndexOf(last);
                final var lastItems = new ArrayList<>(pressureTraverse.subList(startIndex, pressureTraverse.size()));
                pressureTraverse.removeAll(lastItems);
                Point lastOldPoint = null;
                float lastPressure = pumpInitialIntakePressure;
                for (int i = lastItems.size() -1; i >= 0; i--) {
                    if (lastOldPoint == null) {
                        lastOldPoint = lastItems.get(i);
                        continue;
                    }
                    final Point newPoint = lastItems.get(i);
                    final var slope = new Linear(newPoint, lastOldPoint).getSlope();
                    final var newLastPoint = Point.of(lastOldPoint.getX(), lastPressure);
                    final var newLastLine = new Linear(slope, newLastPoint);
                    final var newLastPumpPressure = newLastLine.getY(newPoint.getX());
                    final var newLastPoint2 = Point.of(newPoint.getX(), newLastPumpPressure);
                    lastPressure = newLastPoint2.getY();
                    final int lastIndex = lastItems.indexOf(lastOldPoint);
                    lastItems.set(lastIndex, newLastPoint);
                    lastOldPoint = newPoint;
                }
                if (lastOldPoint != null) {
                    final int lastIndex = lastItems.indexOf(lastOldPoint);
                    lastItems.set(lastIndex, Point.of(lastOldPoint.getX(), lastPressure));
                }
                pressureTraverse.addAll(lastItems);
            }
        } else {
            for (int i = 0; i < pressureTraverse.size(); i++) {
                final var oldPoint = pressureTraverse.get(i);
                pressureTraverse.set(i, Point.of(oldPoint.getX() - totalLength, oldPoint.getY()));
            }
            Collections.reverse(pressureTraverse);
        }
        return pressureTraverse.toArray(new Point[0]);
    }

    private void renderHotTable(List<HotTableRow> hotTableRows) {
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

    private void renderSimplifiedFordHotTable(List<HotTableRow> hotTableRows) {
        final List<Object[]> table = new ArrayList<>();
        table.add(new Object[]{"No.", "Tf1", "Tf2", "Tf", "Ti", "ζi", "Nre", "Flow Type", "C", "F", "hf", "k", "L", "ΣL", "∆P", "Σ∆P"});
        for (int i = 0; i < hotTableRows.size(); i++) {
            final var section = hotTableRows.get(i);
            table.add(new Object[]{i+1, section.getTf1(), section.getTf2(), section.getTfBar(), section.getTi(), section.getVisAtI(),
                    section.getNre(), section.getFlowType(), section.getC(), section.getF(), section.getHf(),
                    section.getK(), section.getL(), section.getSumL(), section.getDeltaP(), section.getSumP()});
        }
        renderTable(table);
    }

    @NotNull
    private HotTableRow calculateSimplifiedFordHotTable(float tf2,
                                                               float tf1,
                                                               float alphaT,
                                                               float deltaT,
                                                               float oD,
                                                               float iD,
                                                               float flowRateM3H,
                                                               float velocityMSec,
                                                               float spGr,
                                                               float tsC,
                                                               float lambdaC,
                                                               float tin,
                                                               float alpha2,
                                                               float visAt100F,
                                                               float visAt212F,
                                                               float totalLength,
                                                               float totalPressure) {



        final float tfBar = (tf2 + tf1)/2; // tf
        final float j = (float) ((Math.log10(Math.log10(visAt100F)) - Math.log10(Math.log10(visAt212F))) / 62);
        final float w = (float) (Math.log10(Math.log10(visAt100F)) + 36 * j);
        final float tI = tfBar - deltaT;
        final float visAtI = (float) Math.pow(10, Math.pow(10, w-j*tI));
        final float pT = 1000 * spGr - alphaT * (tI - 20);
        final float nRe = flowRateM3H / (2827.44f * iD * visAtI * 1e-6f);
        final float f;
        final float alpha1;
        if (nRe <= Constants.LaminarFlowMaxNre) {
            f = 64 / nRe;
            alpha1 = (float) Math.pow(f / 139.7f, -0.3772f);
        } else if (nRe < Constants.TurbulentFlowMaxNre) {
            f = (float) (0.5f / Math.pow(nRe, 0.3f));
            alpha1 = (float) Math.pow(f / 0.0655f, -9.09f);
        } else {
            f = (float) (0.316 / Math.pow(nRe, 0.25));
            alpha1 = (float) Math.pow(f / 0.5604f, -16.835);
        }
        final float k = (float) (Math.PI / (1 / (alpha1 * iD) + Math.log((oD + 0.0508f * tin)/oD) * (0.5f/lambdaC) + 1 / (alpha2 * (oD + 0.0508f * tin))));
        final float c = (float) (762.5f+3.38f*(tI+Constants.ZeroCInKelvin)/Math.sqrt(spGr));
        final float l = (float) (- Math.log((tf2-tsC) / (tf1 - tsC)) * (flowRateM3H * spGr * c) / (3600 * k));
        final float y = totalLength + l;
        final float h = (float) (1000 * f * l * Math.pow(velocityMSec, 2) / (19.6f * iD));
        final float p = h * pT / 10000;
        final float sumPressure = totalPressure + p;
        return new HotTableRow(tf1, tf2, tfBar, tI, visAtI, c, pT, alpha1, l, y, nRe, f, k, h, p, sumPressure);
    }

    @NotNull
    private HotTableRow calculateHotTable(float tf2,
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
        final float c = (float) ((762.5f + 3.38f * (tI+Constants.ZeroCInKelvin)) / Math.sqrt(spGr));
        final float length = (float) (- flowRateM3H * spGr * c * Math.log((tf2 - tsC) / (tf1 - tsC)) / (3600 * k));
        final float sumLength = totalLength + length;
        final float nRe = velocityMSec * iD * 1e+6f / visAtI;
        final float f;
        if (nRe <= Constants.LaminarFlowMaxNre) {
            f = 64 / nRe;
        } else if (nRe < Constants.TurbulentFlowMaxNre) {
            f = (float) (0.5 / Math.pow(nRe, 0.3));
        } else {
            f = (float) (0.316 / Math.pow(nRe, 0.25));
        }
        final float h = (float) (1000 * f * length * Math.pow(velocityMSec, 2) / (19.6f * iD));
        final float p = h * pT / 10000;
        final float sumPressure = totalPressure + p;
        return new HotTableRow(tf1, tf2, tfBar, tI, visAtI, c, pT, null, length, sumLength, nRe, f, k, h, p, sumPressure);
    }

    private PhysicalProperties calculatePhysicalProperties(float iD,
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

        final float mlam = (v * iD * 1e+6f) / Constants.LaminarFlowMaxNre;
        final float mtur = (v * iD * 1e+6f) / Constants.TurbulentFlowMaxNre;
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
        if (nre <= Constants.LaminarFlowMaxNre) {
            if (nGrPr >50000) {
                nu = (float) (0.184 * Math.pow(nGrPr,0.32));
            } else {
                nu = 3.8f;
            }
        } else if (nre < Constants.TurbulentFlowMaxNre) {
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
        return new PhysicalProperties(c, bt, pf, pt, nu, alpha1, alpha2, k, tfBar-ti);
    }

    private void renderTable(List<Object[]> args) {
        renderTable(args.toArray(new Object[0][0]));
    }

    private void renderTable(Object[] ... args) {
        final var temp = args[0];
        final String[] firstRow = new String[temp.length];
        for (int i = 0; i < temp.length; i++) {
            firstRow[i] = temp[i].toString();
        }
        TableList at = new TableList(firstRow).sortBy(0).withUnicode(true);
        final var newRows = Arrays.stream(args).skip(1).map( row -> {
            final String[] newRow = new String[row.length];
            for (int i = 0; i < row.length; i++) {
                newRow[i] = row[i].toString();
            }
            return newRow;
        }).collect(Collectors.toList());
        for (var row: newRows) {
            at.addRow(row);
        }
        String rend = at.render();
        println(rend);
    }

    private void println(@NotNull String pattern, Object... args) {
        final String message = format(pattern, args);
        steps.append(message).append('\n');
        FileUtils.printOut(message);
    }
    
    private void clear() {
        steps = new StringBuilder();
        FileUtils.clear();
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
