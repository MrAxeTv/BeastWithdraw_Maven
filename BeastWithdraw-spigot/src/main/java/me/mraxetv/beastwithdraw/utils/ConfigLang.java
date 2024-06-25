package me.mraxetv.beastwithdraw.utils;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public final class ConfigLang {


    private BeastWithdrawPlugin pl;

    public static String STACK_SIZE;

    public static NumberFormat NUMBER_FORMAT;


    public ConfigLang(BeastWithdrawPlugin pl) {
        this.pl = pl;
        STACK_SIZE = pl.getMessages().getString("PlaceHolders.StackSize");

         if(pl.getConfig().getBoolean("Settings.DisableDecimalAmounts")){
             NUMBER_FORMAT = new DecimalFormat("#" + pl.getConfig().getString("Settings.BalanceFormat", "###,##0"), DecimalFormatSymbols.getInstance(Locale.ENGLISH));
         }else NUMBER_FORMAT = new DecimalFormat("#" + pl.getConfig().getString("Settings.BalanceFormat", "###,##0.##"), DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        NUMBER_FORMAT.setRoundingMode(RoundingMode.DOWN);
    }




}
