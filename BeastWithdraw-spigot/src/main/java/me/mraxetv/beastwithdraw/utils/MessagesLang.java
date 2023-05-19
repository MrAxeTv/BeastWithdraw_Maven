package me.mraxetv.beastwithdraw.utils;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;

public class MessagesLang {

    public static String PREFIX;
    public static String TRANSACTION_FAILED;

    public MessagesLang(BeastWithdrawPlugin pl){

        PREFIX = pl.getMessages().getString("Prefix");
        TRANSACTION_FAILED = pl.getMessages().getString("Withdraws.CashNote.TransactionFailed");

    }


}
