package me.mraxetv.beastwithdraw.utils;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;

public class MessagesLang {

    public static String PREFIX;
    public static String TRANSACTION_FAILED;
    public static String NO_PERMISSIONS;
    public static String STACK_SIZE;
    public MessagesLang(BeastWithdrawPlugin pl){

        PREFIX = pl.getMessages().getString("Prefix");
        TRANSACTION_FAILED = pl.getMessages().getString("Withdraws.CashNote.TransactionFailed");
        NO_PERMISSIONS = pl.getMessages().getString("Withdraws.NoPermission");

        //Placeholders
        STACK_SIZE = pl.getMessages().getString("PlaceHolder.StackSize");


    }


}
