package com.braseiro.core.system;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registro desacoplado de sistemas. Nenhum recurso do Motor Barbara depende de D&D. */
public final class SystemModuleRegistry {
    private final Map<String,GameSystemModule> modules = new LinkedHashMap<String,GameSystemModule>();

    public SystemModuleRegistry register(GameSystemModule module){
        if(module==null||module.id()==null||module.id().trim().isEmpty())throw new IllegalArgumentException("Modulo de sistema invalido");
        modules.put(module.id(),module);
        return this;
    }

    public GameSystemModule require(String id){
        GameSystemModule module=modules.get(id);
        if(module==null)throw new IllegalStateException("Sistema nao registrado: "+id);
        return module;
    }

    public Map<String,GameSystemModule> snapshot(){return Collections.unmodifiableMap(modules);}
}
