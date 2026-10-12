package com.braseiro.dnd5e;

import android.content.Context;

/**
 * Utilitário de teste para instanciar AppDb com nome de banco customizado
 * a partir de testes localizados em subpacotes como com.braseiro.dnd5e.adventure.
 */
public final class AppDbTestHelper {
    private AppDbTestHelper() {}

    public static AppDb createSandbox(Context context, String databaseName) {
        return new AppDb(context, databaseName);
    }
}
