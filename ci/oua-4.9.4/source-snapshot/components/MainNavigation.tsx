import React from 'react';
import { BrainCircuit, Castle, Compass, Dices, FileText, Map, Music, Scroll, Settings, Shield, Sprout, UserCircle, Users, UsersRound } from 'lucide-react';

export type AppTab = 'jogo' | 'mestre' | 'combate' | 'companhia' | 'mundo' | 'poderes' | 'ficha' | 'dados' | 'npcs' | 'sons' | 'manuais' | 'tokens' | 'campanha' | 'configuracoes' | 'personagens';
interface MainNavigationProps { activeTab: AppTab; onTabChange: (tab: AppTab) => void; }
interface TabItem { id: AppTab; label: string; icon: React.ComponentType<any>; }
export const MainNavigation: React.FC<MainNavigationProps> = ({ activeTab, onTabChange }) => {
  const tabs: TabItem[] = [
    { id: 'jogo', label: 'Aventura', icon: Compass },
    { id: 'mestre', label: 'Mestre IA', icon: BrainCircuit },
    { id: 'combate', label: 'Combate', icon: Shield },
    { id: 'companhia', label: 'Companhia', icon: UsersRound },
    { id: 'mundo', label: 'Mundo Vivo', icon: Sprout },
    { id: 'poderes', label: 'Povos & Poderes', icon: Castle },
    { id: 'ficha', label: 'Ficha', icon: FileText },
    { id: 'personagens', label: 'Personagens', icon: UserCircle },
    { id: 'dados', label: 'Dados', icon: Dices },
    { id: 'npcs', label: 'NPCs', icon: Users },
    { id: 'sons', label: 'Sons', icon: Music },
    { id: 'manuais', label: 'Manuais', icon: Scroll },
    { id: 'tokens', label: 'Tokens', icon: UserCircle },
    { id: 'campanha', label: 'Campanha', icon: Map },
    { id: 'configuracoes', label: 'Configurações', icon: Settings },
  ];
  return <nav className="w-full border-b border-blue-900/30 bg-zinc-950/60"><div className="mx-auto max-w-7xl px-2 md:px-4"><ul className="no-scrollbar -mb-px flex select-none items-center gap-1 overflow-x-auto scroll-smooth py-2">{tabs.map(tab=>{const Icon=tab.icon,isActive=activeTab===tab.id;return <li key={tab.id} className="shrink-0"><button onClick={()=>onTabChange(tab.id)} className={`flex min-h-[48px] cursor-pointer items-center gap-1.5 rounded border-2 px-3 py-2 font-serif text-xs font-semibold tracking-wider transition-all duration-200 ${isActive?'border-blue-500 bg-blue-950/30 font-bold text-blue-300 shadow-[0_0_10px_rgba(59,130,246,0.18)]':'border-transparent text-blue-700/80 hover:bg-blue-950/10 hover:text-blue-400'}`}><Icon className={`h-4 w-4 ${isActive?'text-blue-400':'text-blue-800'}`}/><span>{tab.label}</span></button></li>})}</ul></div></nav>;
};
