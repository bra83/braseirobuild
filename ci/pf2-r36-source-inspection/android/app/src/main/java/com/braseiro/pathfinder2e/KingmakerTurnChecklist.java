package com.braseiro.pathfinder2e;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Checklist Interativo do Turno do Reino e Atividades de Liderança (PF2e Kingmaker).
 * Totalmente traduzido para Português (PT-BR), com nomes originais em inglês,
 * líderes autorizados e explicação breve acessível pelo ícone [i].
 */
public final class KingmakerTurnChecklist {

    public static final class ChecklistItem {
        public final String id;
        public final String phase;          // UPKEEP, COMMERCE, LEADERSHIP, REGION, CIVIC, ARMY
        public final String phaseLabel;     // Manutenção, Comércio, Liderança, Região, Cívica, Exército
        public final String namePt;         // Nome em Português
        public final String nameEn;         // Nome original em Inglês
        public final String roles;          // Líderes autorizados
        public final String frequency;      // Frequência / limite
        public final String requirement;    // Destreinado, Treinado, Especialista, Lendário
        public final String brief;          // Breve explicação em PT-BR
        public final String mechanics;      // Regras, Perícias, CDs e Efeitos

        public ChecklistItem(String id, String phase, String phaseLabel, String namePt, String nameEn,
                             String roles, String frequency, String requirement,
                             String brief, String mechanics) {
            this.id = id;
            this.phase = phase;
            this.phaseLabel = phaseLabel;
            this.namePt = namePt;
            this.nameEn = nameEn;
            this.roles = roles;
            this.frequency = frequency;
            this.requirement = requirement;
            this.brief = brief;
            this.mechanics = mechanics;
        }
    }

    private static final List<ChecklistItem> ITEMS = new ArrayList<>();

    static {
        // ==========================================
        // 1. MANUTENÇÃO (UPKEEP PHASE) - 5 Atividades
        // ==========================================
        add("gain_fame", "UPKEEP", "Manutenção", "Ganhar 1 Fama", "Gain 1 Fame",
            "Automático / Todos os Líderes", "1 vez por turno (início da Manutenção)", "Automático",
            "O reino ganha 1 Ponto de Fama (ou Infâmia, se aplicável). Pontos de Fama podem ser gastos posteriormente para rolar novamente testes de perícia do Reino ou auxiliar em eventos cruciais.",
            "Kingmaker AP p. 536. Adiciona 1 Ponto de Fama ao tesouro do reino. A Fama máxima é limitada pelo Nível do Reino.");

        add("new_leadership", "UPKEEP", "Manutenção", "Nova Liderança", "New Leadership",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "Na Manutenção ou sob vacância/baixa", "Destreinado",
            "Permite preencher cargos de liderança vagos ou reatribuir funções entre os personagens jogadores e companheiros. Cargos desocupados impõem penalidades contínuas às perícias do Reino.",
            "Kingmaker AP p. 520, 537. Teste de Intriga, Política, Estadismo ou Guerra contra a CD de Controle do Reino. Sucesso Crítico: +1 de bônus em atividades do líder. Sucesso: nomeação aceita. Falha: aprovação pendente (-1 em atividades). Falha Crítica: rejeição popular, +1 Agitação.");

        add("adjust_unrest", "UPKEEP", "Manutenção", "Ajustar Agitação", "Adjust Unrest",
            "Governante (ou Automático)", "1 vez por turno", "Automático",
            "Avalia o estado de descontentamento social do reino. Se o reino estiver estável e sem eventos desastrosos, a Agitação pode ser reduzida. Agitação elevada penaliza todos os testes do reino.",
            "Kingmaker AP p. 537. Se a Agitação for 0 no início do turno, o reino ganha 1 Ponto de Recursos de bônus. Se a Agitação atingir 20, o reino entra em Anarquia.");

        add("collect_resources", "UPKEEP", "Manutenção", "Coletar Recursos", "Collect Resources",
            "Automático / Todos os Líderes", "1 vez por turno", "Automático",
            "Role os Dados de Recursos (DR) do reino e adicione os Pontos de Recursos (PR) ao Tesouro. Locais de trabalho (minas, acampamentos madeireiros, pedreiras) e bônus de líderes concedem dados adicionais.",
            "Kingmaker AP p. 538. Role o número de Dados de Recursos baseado no Nível e tamanho do reino. Recursos acumulados não gastos além da capacidade de armazenamento do Tesouro são perdidos.");

        add("pay_consumption", "UPKEEP", "Manutenção", "Pagar Consumo", "Pay Consumption",
            "Automático / Todos os Líderes", "1 vez por turno", "Automático",
            "Deduz-se o Consumo total de alimentos e manutenção do suprimento de Alimentos ou dos Pontos de Recursos (PR). Reduzido por fazendas e certas estruturas municipais.",
            "Kingmaker AP p. 538. Consumo = Tamanho do Reino + número de assentamentos mantidos - Fazendas e Moinhos. Se o reino não tiver recursos suficientes para pagar o Consumo, sofre +1d4 de Agitação.");

        // ==========================================
        // 2. COMÉRCIO (COMMERCE PHASE) - 5 Atividades
        // ==========================================
        add("collect_taxes", "COMMERCE", "Comércio", "Coletar Impostos (Treinado)", "Collect Taxes (Trained)",
            "Tesoureiro (primário), Governante, Conselheiro, Emissário, General, Magister, Vice-Rei, Guardião", "1 vez por fase de Comércio", "Treinado",
            "Arrecada tributos, taxas alfandegárias e impostos da população e comerciantes locais para abastecer o Tesouro do Reino.",
            "Kingmaker AP p. 539. Teste de Indústria contra a CD de Controle. Sucesso Crítico: Ganha PR igual ao dobro do Nível do Reino. Sucesso: Ganha PR igual ao Nível do Reino. Falha: Ganha 1 PR. Falha Crítica: Não arrecada nada e ganha +1 de Agitação.");

        add("improve_lifestyle", "COMMERCE", "Comércio", "Melhorar Estilo de Vida", "Improve Lifestyle",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "1 vez por fase de Comércio", "Destreinado",
            "Os líderes investem fundos pessoais ou financiam melhorias para elevar o conforto e bem-estar dos cidadãos, reduzindo descontentamento.",
            "Kingmaker AP p. 539. Gasta recursos para conceder bônus de circunstância a testes de Sociedade e Cultura do reino durante a rodada e manter a moral elevada.");

        add("tap_treasury", "COMMERCE", "Comércio", "Usar Tesouro", "Tap Treasury",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "1 vez por fase de Comércio", "Destreinado",
            "Retirar fundos dos cofres públicos do reino e convertê-los em moedas de ouro pessoais para os líderes aventureiros, arriscando revolta popular.",
            "Kingmaker AP p. 539. Converte PR em peças de ouro pessoais para o grupo. Falha ou uso abusivo gera Agitação imediata e penalidades em futuras coletas de impostos.");

        add("trade_commodities", "COMMERCE", "Comércio", "Comercializar Mercadorias", "Trade Commodities",
            "Emissário, Tesoureiro, Vice-Rei", "1 vez por fase de Comércio", "Destreinado",
            "Exportar mercadorias excedentes (Comida, Madeira, Pedra, Minério, Luxos) para mercados vizinhos em troca de outras mercadorias ou PR.",
            "Kingmaker AP p. 539. Teste de Comércio contra a CD de Controle. Permite trocar até o limite permitido de unidades de matéria-prima por taxa favorável.");

        add("manage_trade_agreements", "COMMERCE", "Comércio", "Gerenciar Acordos Comerciais", "Manage Trade Agreements",
            "Emissário (primário), Tesoureiro, Governante, Conselheiro, General, Magister, Vice-Rei, Guardião", "1 vez por fase de Comércio", "Destreinado",
            "Supervisionar rotas e acordos comerciais ativos com nações parceiras (Restov, Brevoy, Mivon), assegurando entregas e bônus econômicos contínuos.",
            "Kingmaker AP p. 540. Teste de Comércio ou Diplomacia. Sucesso mantém e aprimora tratados que geram PR e itens raros para os mercados da capital.");

        // ==========================================
        // 3. LIDERANÇA (LEADERSHIP PHASE) - 25 Atividades (3 por jogador)
        // ==========================================
        add("capital_investment", "LEADERSHIP", "Liderança", "Investimento de Capital", "Capital Investment",
            "Governante, Emissário, Tesoureiro, Vice-Rei, Guardião", "3 ações por jogador", "Destreinado",
            "Um líder injeta riqueza pessoal (moedas de ouro de aventureiro) no Tesouro do reino para financiar projetos imediatos.",
            "Kingmaker AP p. 540. Converte PO pessoal em Pontos de Recursos (PR) à taxa definida pelo Nível do Reino, sem teste.");

        add("celebrate_holiday", "LEADERSHIP", "Liderança", "Celebrar Feriado", "Celebrate Holiday",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "3 ações por jogador", "Destreinado",
            "Promove festivais cívicos, jogos públicos ou celebrações religiosas para elevar os ânimos do povo e aplacar conflitos.",
            "Kingmaker AP p. 540. Gasta PR e Comida. Teste de Artes ou Folclore contra a CD de Controle. Sucesso: reduz Agitação e concede bônus temporário em testes de Moral.");

        add("clandestine_business", "LEADERSHIP", "Liderança", "Negócios Clandestinos (Treinado)", "Clandestine Business (Trained)",
            "General, Magister, Vice-Rei, Guardião", "3 ações por jogador", "Treinado",
            "Utiliza informantes, contrabandistas e redes do submundo para obter recursos secretos ou enfraquecer oponentes discretamente.",
            "Kingmaker AP p. 540. Teste de Intriga contra a CD de Controle. Sucesso gera PR rápidos ou bônus de espionagem, mas falha crítica aumenta a Ruína de Crime.");

        add("craft_luxuries", "LEADERSHIP", "Liderança", "Fabricar Luxos", "Craft Luxuries",
            "Governante", "3 ações por jogador", "Destreinado",
            "Supervisiona oficinas e artífices para produzir bens de alto valor estético e prestígio, destinados a banquetes, tratados ou exportação.",
            "Kingmaker AP p. 541. Teste de Manufatura contra a CD de Controle. Sucesso gera unidades de Luxo no inventário do reino.");

        add("create_masterpiece", "LEADERSHIP", "Liderança", "Criar Obra-Prima (1/Rodada, Treinado)", "Create a Masterpiece (Once per Round, Trained)",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "1 vez por rodada", "Treinado",
            "Financia grandes mestres, arquitetos ou poetas para criar uma obra cultural histórica e imortalizar o nome do reino.",
            "Kingmaker AP p. 541. Teste de Artes contra a CD de Controle. Sucesso Crítico: +1 Fama permanente e bônus duradouro de Cultura. Sucesso: +1 Fama.");

        add("creative_solution", "LEADERSHIP", "Liderança", "Solução Criativa 🍀", "Creative Solution",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "3 ações por jogador", "Destreinado",
            "Aplica uma abordagem inovadora e perspicaz para contornar um problema burocrático ou evento adverso que ameace o reino.",
            "Kingmaker AP p. 541. Gasta Fama para substituir um teste padrão de reino por uma perícia onde o líder possui maior graduação.");

        add("establish_trade_agreement", "LEADERSHIP", "Liderança", "Estabelecer Acordo Comercial", "Establish Trade Agreement",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "3 ações por jogador", "Destreinado",
            "Negocia um novo pacto de comércio oficial com governos estrangeiros ou guildas mercantis distantes.",
            "Kingmaker AP p. 541. Teste de Comércio ou Relações Exteriores. Sucesso abre rota comercial com bônus fixos mensais de PR.");

        add("focused_attention", "LEADERSHIP", "Liderança", "Atenção Focada", "Focused Attention",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "3 ações por jogador", "Destreinado",
            "O líder dedica seus esforços integrais a supervisionar um setor do governo, garantindo excelência operacional.",
            "Kingmaker AP p. 541. Concede bônus de circunstância de +2 em todos os testes de uma perícia específica do Reino até o próximo turno.");

        add("hire_adventurers", "LEADERSHIP", "Liderança", "Contratar Aventureiros", "Hire Adventurers",
            "Conselheiro, Emissário, General, Magister, Vice-Rei, Guardião", "3 ações por jogador", "Destreinado",
            "Publica editais e contrata mercenários ou grupos de heróis para explorar rumores remotos ou caçar monstros nos ermos.",
            "Kingmaker AP p. 542. Teste de Política ou Guerra. Sucesso: os aventureiros resolvem um perigo regional menor ou reconhecem um hex sem expor os PJs.");

        add("infiltration", "LEADERSHIP", "Liderança", "Infiltração", "Infiltration",
            "Conselheiro, Emissário, General, Magister, Vice-Rei, Guardião", "3 ações por jogador", "Destreinado",
            "Envia agentes secretos para espionar acampamentos inimigos, fortalezas rivais ou cortes estrangeiras.",
            "Kingmaker AP p. 542. Teste de Intriga contra a CD de Controle do alvo. Sucesso revela planos de guerra inimigos, contingentes e fortificações.");

        add("pledge_of_fealty", "LEADERSHIP", "Liderança", "Juramento de Lealdade (Treinado)", "Pledge of Fealty (Trained)",
            "Conselheiro, Emissário, General, Magister, Vice-Rei, Guardião", "3 ações por jogador", "Treinado",
            "Aceita formalmente a subserviência pacífica de tribos locais (como kobolds Sootscale ou homens-lagarto) como cidadãos do reino.",
            "Kingmaker AP p. 542. Teste de Política ou Diplomacia contra CD especial. Sucesso integra a comunidade sem combate, concedendo hexes limpos e novos súditos.");

        add("prognostication", "LEADERSHIP", "Liderança", "Prognóstico (Treinado)", "Prognostication (Trained)",
            "Magister, Conselheiro", "3 ações por jogador", "Treinado",
            "Consulta oráculos, mapas estelares ou rituais divinos para antever acontecimentos futuros e proteger o reino de crises.",
            "Kingmaker AP p. 542. Teste de Magia ou Folclore. Sucesso concede bônus contra o próximo Evento do Reino ou revela a natureza de uma maldição.");

        add("provide_care", "LEADERSHIP", "Liderança", "Prestar Cuidados / Assistência", "Provide Care",
            "Conselheiro, Magister, Guardião", "3 ações por jogador", "Destreinado",
            "Organiza socorro a comunidades afetadas por desastres, epidemias, pragas ou ataques de monstros nos assentamentos.",
            "Kingmaker AP p. 542. Teste de Erudição ou Medicina contra a CD de Controle. Sucesso reduz a Agitação e previne o avanço de pragas.");

        add("purchase_commodities", "LEADERSHIP", "Liderança", "Comprar Mercadorias", "Purchase Commodities",
            "Governante, Conselheiro, Emissário, General, Magister, Treasurer, Viceroy, Warden", "3 ações por jogador", "Destreinado",
            "Compra remessas de emergência de Madeira, Pedra ou Minério para acelerar construções cívicas urgentes.",
            "Kingmaker AP p. 542. Converte PR diretamente em estoques físicos de materiais de construção no Tesouro.");

        add("quell_unrest", "LEADERSHIP", "Liderança", "Acalmar Agitação (1/Rodada)", "Quell Unrest (Once Per Round)",
            "Governante, Conselheiro, Emissário, General, Magister, Treasurer, Viceroy, Warden", "1 vez por rodada", "Destreinado",
            "O líder faz um discurso memorável, ouve as queixas dos cidadãos e faz concessões políticas para restaurar a ordem.",
            "Kingmaker AP p. 543. Teste de Política ou Artes contra a CD de Controle. Sucesso: reduz a Agitação em 1d4. Sucesso Crítico: reduz em 1d4+2.");

        add("recruit_army", "LEADERSHIP", "Liderança", "Recrutar Exército", "Recruit Army",
            "General, Guardião", "3 ações por jogador", "Destreinado",
            "Convoca soldados entre os cidadãos de um assentamento com quartel para formar uma nova unidade militar registrada no reino.",
            "Kingmaker AP p. 543. Paga custo em PR e materiais. Cria ficha de exército com tipo, moral, equipamentos e nível baseados no assentamento.");

        add("relocate_capital", "LEADERSHIP", "Liderança", "Mudar a Capital (Treinado)", "Relocate Capital (Trained)",
            "Governante, Conselheiro, Emissário, General, Magister, Treasurer, Viceroy, Warden", "3 ações por jogador", "Treinado",
            "Transfere a sede do governo para outro assentamento do reino mais seguro, desenvolvido ou geograficamente vantajoso.",
            "Kingmaker AP p. 543. Teste de Estadismo contra a CD de Controle + 5. Custa PR substanciais e reatribui os bônus de liderança à nova sede.");

        add("repair_reputation_corruption", "LEADERSHIP", "Liderança", "Reparar Reputação: Corrupção (Treinado)", "Repair Reputation Corruption (Trained)",
            "Governante, Conselheiro, Emissário, General, Magister, Treasurer, Viceroy, Warden", "3 ações por jogador", "Treinado",
            "Investiga e pune magistrados corruptos e fraudadores, restaurando a dignidade ética da corte.",
            "Kingmaker AP p. 543. Teste de Política ou Intriga contra a CD de Ruína. Sucesso reduz a pontuação de Ruína de Corrupção em 1 ponto.");

        add("repair_reputation_crime", "LEADERSHIP", "Liderança", "Reparar Reputação: Crime (Treinado)", "Repair Reputation Crime (Trained)",
            "Governante, Conselheiro, Emissário, General, Magister, Treasurer, Viceroy, Warden", "3 ações por jogador", "Treinado",
            "Organiza operações de patrulha e combate a guildas de ladrões e contrabandistas.",
            "Kingmaker AP p. 543. Teste de Guerra ou Intriga contra a CD de Ruína. Sucesso reduz a pontuação de Ruína de Crime em 1 ponto.");

        add("repair_reputation_decay", "LEADERSHIP", "Liderança", "Reparar Reputação: Decadência (Treinado)", "Repair Reputation Decay (Trained)",
            "Governante, Conselheiro, Emissário, General, Magister, Treasurer, Viceroy, Warden", "3 ações por jogador", "Treinado",
            "Restaura monumentos abandonados, redes de esgoto e infraestruturas urbanas deterioradas.",
            "Kingmaker AP p. 544. Teste de Manufatura ou Estadismo contra a CD de Ruína. Sucesso reduz a pontuação de Ruína de Decadência em 1 ponto.");

        add("repair_reputation_strife", "LEADERSHIP", "Liderança", "Reparar Reputação: Conflito (Treinado)", "Repair Reputation Strife (Trained)",
            "Governante, Conselheiro, Emissário, General, Magister, Treasurer, Viceroy, Warden", "3 ações por jogador", "Treinado",
            "Media rivalidades entre clãs nobres, guildas em conflito ou facções religiosas beligerantes.",
            "Kingmaker AP p. 544. Teste de Política ou Folclore contra a CD de Ruína. Sucesso reduz a pontuação de Ruína de Conflito em 1 ponto.");

        add("request_foreign_aid", "LEADERSHIP", "Liderança", "Solicitar Ajuda Externa (Treinado)", "Request Foreign Aid (Trained)",
            "Governante, Conselheiro, Emissário, General, Magister, Treasurer, Viceroy, Warden", "3 ações por jogador", "Treinado",
            "Despacha apelos de auxílio militar, empréstimos de emergência ou mantimentos para aliados em Brevoy ou Mivon.",
            "Kingmaker AP p. 544. Teste de Relações Exteriores. Sucesso fornece recursos ou reforços militares mediante obrigações diplomáticas futuras.");

        add("rest_and_relax", "LEADERSHIP", "Liderança", "Descansar e Relaxar", "Rest and Relax",
            "Todos os Líderes", "3 ações por jogador", "Destreinado",
            "O líder se afasta temporariamente dos deveres de estado para caçar, passear, meditar e renovar o vigor físico e mental.",
            "Kingmaker AP p. 544. Concede bônus de circunstância pessoal em testes de resistência e remove condições de fadiga acumuladas.");

        add("send_diplomatic_envoy", "LEADERSHIP", "Liderança", "Enviar Enviado Diplomático (Treinado)", "Send Diplomatic Envoy (Trained)",
            "Governante, Conselheiro, Emissário, General, Magister, Treasurer, Viceroy, Warden", "3 ações por jogador", "Treinado",
            "Envia embaixadores para forjar tratados de paz, alianças defensivas ou pactos comerciais de longo prazo.",
            "Kingmaker AP p. 544. Teste de Relações Exteriores. Sucesso melhora a atitude diplomática da facção ou nação visada.");

        add("supernatural_solution", "LEADERSHIP", "Liderança", "Solução Sobrenatural 🍀", "Supernatural Solution",
            "Magister", "3 ações por jogador", "Destreinado",
            "Emprega rituais arcanos ou milagres divinos para solucionar um impasse aparentemente impossível pela via comum.",
            "Kingmaker AP p. 545. Gasta Fama e permite usar Magia para substituir testes de reino em crises extraordinárias.");

        // ==========================================
        // 4. REGIÃO (REGION PHASE) - 14 Atividades (3 no total)
        // ==========================================
        add("abandon_hex", "REGION", "Região", "Abandonar Hexágono", "Abandon Hex",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei", "3 ações no total da fase de Região", "Destreinado",
            "Renuncia formalmente à soberania sobre um hexágono distante para reduzir custos de manutenção ou focar as fronteiras.",
            "Kingmaker AP p. 545. Reduz o Tamanho do Reino em 1 hexágono, diminuindo o Consumo. Aumenta a Agitação em 1 se o hex possuir melhorias.");

        add("build_roads", "REGION", "Região", "Construir Estradas", "Build Roads",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "3 ações no total", "Destreinado",
            "Abre estradas de terra ou pavimentadas ligando os hexágonos do reino, facilitando a marcha de exércitos e o comércio.",
            "Kingmaker AP p. 545. Custa PR e Pedra. Reduz tempo de viagem no hexágono e é pré-requisito para pontes e rotas mercantis eficientes.");

        add("claim_hex", "REGION", "Região", "Reivindicar Hexágono (1/Turno)", "Claim Hex (Once Per Turn)",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "1 vez por turno", "Destreinado",
            "Anexa um hexágono adjacente, previamente reconhecido e limpo de ameaças, às fronteiras soberanas do reino.",
            "Kingmaker AP p. 545. Paga o custo em PR baseado no tipo de terreno e fronteira. Aumenta o Tamanho do Reino em 1.");

        add("clear_hex", "REGION", "Região", "Limpar / Desbravar Hexágono", "Clear Hex",
            "General, Guardião", "3 ações no total", "Destreinado",
            "Elimina perigos ambientais, drenagem de pântanos ou desmatamento de clareiras para permitir estradas, fazendas ou assentamentos.",
            "Kingmaker AP p. 545. Teste de Engenharia ou Guerra contra a CD de perigo regional. Hex fica pronto para receber obras.");

        add("establish_farmland", "REGION", "Região", "Estabelecer Terras Agrícolas", "Establish Farmland",
            "General, Guardião", "3 ações no total", "Destreinado",
            "Transforma um hexágono de planícies ou colinas limpas em plantações cultivadas para alimentar o povo e reduzir o Consumo.",
            "Kingmaker AP p. 546. Custa PR e exige hex limpo com acesso a água ou irrigação. Reduz o Consumo do Reino em 1.");

        add("establish_settlement", "REGION", "Região", "Fundar Assentamento", "Establish Settlement",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "3 ações no total", "Destreinado",
            "Funda uma nova vila em um hexágono do reino, abrindo uma malha urbana de lotes para construção de edifícios cívicos.",
            "Kingmaker AP p. 546. Custa PR e madeira/pedra. Cria um novo assentamento no AppDb com 1 distrito inicial e 36 lotes.");

        add("establish_work_site_lumber_camp", "REGION", "Região", "Estabelecer Acampamento Madeireiro", "Establish Work Site Lumber Camp",
            "General, Guardião", "3 ações no total", "Destreinado",
            "Constrói um centro permanente de corte e serraria em um hexágono florestal para abastecer o reino de Madeira.",
            "Kingmaker AP p. 546. Fornece produção contínua de Madeira todo turno do Reino, adicionando Dados de Recursos ao Tesouro.");

        add("establish_work_site_mine", "REGION", "Região", "Estabelecer Mina", "Establish Work Site Mine",
            "General, Guardião", "3 ações no total", "Destreinado",
            "Abre galerias de extração mineral em montanhas ou colinas com jazidas comprovadas para fornecer Minério ao reino.",
            "Kingmaker AP p. 546. Fornece produção contínua de Minério todo turno, acelerando armas, armaduras e construções de alvenaria.");

        add("establish_work_site_quarry", "REGION", "Região", "Estabelecer Pedreira", "Establish Work Site Quarry",
            "General, Guardião", "3 ações no total", "Destreinado",
            "Instala pedreira e talhadeiras em afloramentos rochosos para fornecer blocos de Pedra de construção para muralhas e catedrais.",
            "Kingmaker AP p. 546. Fornece produção regular de Pedra para as construções cívicas e fortificações do reino.");

        add("fortify_hex", "REGION", "Região", "Fortificar Hexágono", "Fortify Hex",
            "General, Guardião", "3 ações no total", "Destreinado",
            "Ergue torreões de vigia, baluartes e paliçadas em um hexágono de fronteira para defender contra invasões e dar abrigo às patrulhas.",
            "Kingmaker AP p. 547. Exige Pedra e Madeira. Concede bônus de Defesa a exércitos amigos posicionados no hex e dificulta passagens inimigas.");

        add("gather_livestock", "REGION", "Região", "Reunir Gado", "Gather Livestock",
            "General, Guardião", "3 ações no total", "Destreinado",
            "Conduz rebanhos bovinos e equinos para pastagens seguras do reino, gerando mantimentos e montarias para cavalaria.",
            "Kingmaker AP p. 547. Teste de Natureza ou Agricultura. Sucesso fornece Comida extra e reduz custos de equipar unidades de cavalaria.");

        add("go_fishing", "REGION", "Região", "Pescar", "Go Fishing",
            "General, Guardião", "3 ações no total", "Destreinado",
            "Organiza cooperativas pesqueiras e frotas fluviais nos lagos e rios navegáveis das Stolen Lands.",
            "Kingmaker AP p. 547. Exige hex com rio navegável ou lago. Teste de Sobrevivência. Sucesso adiciona Comida ao Tesouro.");

        add("harvest_crops", "REGION", "Região", "Colher Safras", "Harvest Crops",
            "General, Guardião", "3 ações no total", "Destreinado",
            "Mobiliza os camponeses para colheita intensa nas fazendas no outono, enchendo os celeiros para o inverno.",
            "Kingmaker AP p. 547. Teste de Agricultura. Sucesso Crítico fornece reservas abundantes de Comida para suportar meses de seca ou frio.");

        add("irrigation", "REGION", "Região", "Irrigação (Treinado)", "Irrigation (Trained)",
            "General, Guardião", "3 ações no total", "Treinado",
            "Abre aquedutos e canais fluviais para levar água a hexágonos de planície seca, permitindo a criação de novas terras agrícolas.",
            "Kingmaker AP p. 547. Teste de Engenharia. Sucesso permite converter hexágonos áridos em Fazendas produtivas.");

        // ==========================================
        // 5. CÍVICA (CIVIC PHASE) - 2 Atividades (1 por assentamento)
        // ==========================================
        add("build_structure", "CIVIC", "Cívica", "Construir Estrutura", "Build Structure",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "1 por assentamento", "Destreinado",
            "Ergue uma nova estrutura oficial (Casas, Taverna, Quartel, Muralha, Templo, etc.) em lotes livres do assentamento.",
            "Kingmaker AP pp. 544-552. Paga custos em PR, Madeira, Pedra e Minério. Concede bônus permanentes a perícias do reino, reduz consumo ou destrava serviços.");

        add("demolish", "CIVIC", "Cívica", "Demolir", "Demolish",
            "Governante, Conselheiro, Emissário, General, Magister, Tesoureiro, Vice-Rei, Guardião", "1 por assentamento", "Destreinado",
            "Desmonta uma estrutura municipal obsoleta ou danificada para desocupar lotes ou reaproveitar metade dos materiais originais.",
            "Kingmaker AP p. 548. Libera os lotes no assentamento e devolve 50% dos materiais de construção ao Tesouro.");

        // ==========================================
        // 6. EXÉRCITO (ARMY PHASE) - 12 Atividades (1 por exército)
        // ==========================================
        add("deploy_army", "ARMY", "Exército", "Desdobrar Exército", "Deploy Army",
            "General, Guardião", "1 por exército", "Destreinado",
            "Mobiliza a unidade militar de sua guarnição para marchar pelo mapa, patrulhar fronteiras ou avançar contra exércitos inimigos.",
            "Kingmaker AP p. 548. Exército move-se pelo mapa de hexágonos respeitando custo de movimento de terreno e estradas.");

        add("disband_army", "ARMY", "Exército", "Desmantelar Exército (Lendário)", "Disband Army (Legendary)",
            "General, Guardião", "1 por exército", "Lendário",
            "Desmobiliza formalmente uma unidade veterana com honras, devolvendo os soldados aos seus lares e eliminando seu consumo militar contínuo.",
            "Kingmaker AP p. 548. Elimina o custo de consumo do exército e devolve veteranos à força de trabalho municipal.");

        add("garrison_army", "ARMY", "Exército", "Guarnecer Exército", "Garrison Army",
            "General, Guardião", "1 por exército", "Destreinado",
            "Estaciona o exército em um assentamento ou hex fortificado para atuar como guarda local, conter distúrbios e elevar a segurança cívica.",
            "Kingmaker AP p. 548. O exército assume postura defensiva, concedendo bônus de defesa e contendo aumento de Agitação na cidade.");

        add("offensive_gambit", "ARMY", "Exército", "Manobra Ofensiva", "Offensive Gambit",
            "General, Guardião", "1 por exército", "Destreinado",
            "Comanda uma investida tática audaciosa, cerco relâmpago ou assalto surpresa contra posições inimigas durante o combate bélico.",
            "Kingmaker AP p. 549. Teste de Táticas Militares. Concede bônus de dano ou vantagem posicional contra exércitos adversários.");

        add("outfit_army", "ARMY", "Exército", "Equipar Exército", "Outfit Army",
            "General, Guardião", "1 por exército", "Destreinado",
            "Distribui armas de ferro frio, armaduras forjadas, arcos compostos ou poções curativas para aprimorar o poder bélico da tropa.",
            "Kingmaker AP p. 549. Paga custo em Minério e PR. Aumenta o Ataque ou a Defesa do exército permanentemente.");

        add("recover_army_damaged", "ARMY", "Exército", "Recuperar Exército: Danificado", "Recover Army: Damaged",
            "General, Guardião", "1 por exército", "Destreinado",
            "Concede repouso, cirurgiões de campo e cuidados veterinários para curar Pontos de Vida perdidos pela tropa após a batalha.",
            "Kingmaker AP p. 549. Teste de Medicina ou Guerra contra a CD da batalha. Sucesso recupera Pontos de Vida do exército.");

        add("recover_army_defeated", "ARMY", "Exército", "Recuperar Exército: Derrotado (Especialista)", "Recover Army: Defeated (Expert)",
            "General, Guardião", "1 por exército", "Especialista",
            "Reorganiza batalhões destroçados e reagrupa soldados dispersos após uma derrota severa ou debandada.",
            "Kingmaker AP p. 549. Teste de Liderança Militar contra CD difícil. Sucesso restaura o exército como unidade combatente com PV reduzidos.");

        add("recover_army_lost", "ARMY", "Exército", "Recuperar Exército: Perdido", "Recover Army: Lost",
            "General, Guardião", "1 por exército", "Destreinado",
            "Despacha mensageiros e patrulhas com batedores experientes para resgatar tropas extraviadas em terrenos hostis.",
            "Kingmaker AP p. 550. Teste de Sobrevivência ou Reconhecimento. Sucesso localiza a unidade e a reconduz a território amigo.");

        add("recover_army_mired_pinned", "ARMY", "Exército", "Recuperar Exército: Atolado ou Preso", "Recover Army: Mired or Pinned",
            "General, Guardião", "1 por exército", "Destreinado",
            "Constrói paliçadas de passagem, pontes improvisadas ou apoios de alavanca para resgatar tropas retidas em atoleiros ou terrenos difíceis.",
            "Kingmaker AP p. 550. Teste de Engenharia ou Guerra. Sucesso remove a condição Atolado / Preso da unidade.");

        add("recover_army_shaken", "ARMY", "Exército", "Recuperar Exército: Abalado", "Recover Army: Shaken",
            "General, Guardião", "1 por exército", "Destreinado",
            "Capelães militares e oficiais discursam e realizam ritos para restaurar o ânimo e a coragem de soldados amedrontados.",
            "Kingmaker AP p. 550. Teste de Moral militar. Sucesso remove a condição Abalado e restaura a disciplina da tropa.");

        add("recover_army_weary", "ARMY", "Exército", "Recuperar Exército: Cansado", "Recover Army: Weary",
            "General, Guardião", "1 por exército", "Destreinado",
            "Estabelece acampamento ordenado com boa alimentação e descanso completo após marchas forçadas e combate extenuante.",
            "Kingmaker AP p. 550. Sucesso remove a condição de Cansaço militar, restaurando as penalidades de marcha e combate.");

        add("train_army", "ARMY", "Exército", "Treinar Exército (máx 1/exército)", "Train Army (max 1 per army)",
            "General, Guardião", "máximo 1 vez por exército por turno", "Destreinado",
            "Conduz exercícios táticos no campo de batalha para ensinar manobras especializadas ou elevar o nível de proficiência militar da tropa.",
            "Kingmaker AP p. 550. Teste de Guerra. Sucesso concede uma nova tática de combate ou aprimoramento de experiência à unidade militar.");
    }

    private static void add(String id, String phase, String phaseLabel, String namePt, String nameEn,
                            String roles, String frequency, String requirement, String brief, String mechanics) {
        ITEMS.add(new ChecklistItem(id, phase, phaseLabel, namePt, nameEn, roles, frequency, requirement, brief, mechanics));
    }

    public static List<ChecklistItem> getItems() {
        return Collections.unmodifiableList(ITEMS);
    }

    public static List<ChecklistItem> getItemsForPhase(String phase) {
        if (phase == null || "ALL".equalsIgnoreCase(phase) || "TODAS".equalsIgnoreCase(phase)) {
            return getItems();
        }
        List<ChecklistItem> filtered = new ArrayList<>();
        for (ChecklistItem item : ITEMS) {
            if (item.phase.equalsIgnoreCase(phase)) {
                filtered.add(item);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    /**
     * Exibe o diálogo interativo do Checklist do Reino.
     */
    public static void showDialog(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        final SharedPreferences prefs = activity.getSharedPreferences("kingmaker_checklist", Context.MODE_PRIVATE);
        final float density = activity.getResources().getDisplayMetrics().density;
        final int pad8 = (int) (8 * density);
        final int pad12 = (int) (12 * density);
        final int pad16 = (int) (16 * density);

        final LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad16, pad12, pad16, pad16);
        root.setBackgroundColor(Color.rgb(20, 20, 26));

        // Subtítulo descritivo
        TextView sub = new TextView(activity);
        sub.setText("Marque as ações realizadas no turno. Clique no [ ℹ ] em cada tópico para ver regras, líderes e detalhes.");
        sub.setTextColor(Color.rgb(180, 180, 190));
        sub.setTextSize(12f);
        sub.setPadding(0, 0, 0, pad8);
        root.addView(sub);

        // Barra de filtros por Fase
        HorizontalScrollView hScroll = new HorizontalScrollView(activity);
        hScroll.setHorizontalScrollBarEnabled(false);
        final LinearLayout filterBar = new LinearLayout(activity);
        filterBar.setOrientation(LinearLayout.HORIZONTAL);

        final String[] phases = {"TODAS", "UPKEEP", "COMMERCE", "LEADERSHIP", "REGION", "CIVIC", "ARMY"};
        final String[] phaseLabels = {"Todas (63)", "Manutenção (5)", "Comércio (5)", "Liderança (25)", "Região (14)", "Cívica (2)", "Exército (12)"};

        final LinearLayout itemsContainer = new LinearLayout(activity);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);

        final TextView[] filterButtons = new TextView[phases.length];

        final Runnable populateItems = new Runnable() {
            String selectedPhase = "TODAS";
            @Override
            public void run() {
                itemsContainer.removeAllViews();
                List<ChecklistItem> current = getItemsForPhase(selectedPhase);
                String lastPhase = "";

                for (final ChecklistItem item : current) {
                    // Separador de Fase
                    if (!item.phase.equals(lastPhase)) {
                        lastPhase = item.phase;
                        TextView phaseHeader = new TextView(activity);
                        phaseHeader.setText("— FASE: " + item.phaseLabel.toUpperCase() + " —");
                        phaseHeader.setTextColor(Color.rgb(212, 175, 55)); // GOLD
                        phaseHeader.setTextSize(13f);
                        phaseHeader.setTypeface(Typeface.DEFAULT_BOLD);
                        phaseHeader.setPadding(0, pad12, 0, pad8);
                        itemsContainer.addView(phaseHeader);
                    }

                    // Linha da Atividade
                    LinearLayout row = new LinearLayout(activity);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(0, pad8, 0, pad8);

                    // CheckBox
                    final CheckBox cb = new CheckBox(activity);
                    boolean isChecked = prefs.getBoolean("chk_" + item.id, false);
                    cb.setChecked(isChecked);
                    cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                            prefs.edit().putBoolean("chk_" + item.id, checked).apply();
                        }
                    });
                    row.addView(cb);

                    // Botão [ ℹ ]
                    TextView infoBtn = new TextView(activity);
                    infoBtn.setText(" ℹ ");
                    infoBtn.setTextColor(Color.rgb(212, 175, 55));
                    infoBtn.setTextSize(14f);
                    infoBtn.setTypeface(Typeface.DEFAULT_BOLD);
                    infoBtn.setPadding(pad8, pad8, pad8, pad8);
                    infoBtn.setBackgroundColor(Color.rgb(35, 35, 45));
                    infoBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showItemDetailsDialog(activity, item);
                        }
                    });
                    row.addView(infoBtn);

                    // Textos do Item
                    LinearLayout textCol = new LinearLayout(activity);
                    textCol.setOrientation(LinearLayout.VERTICAL);
                    textCol.setPadding(pad8, 0, 0, 0);
                    LinearLayout.LayoutParams lpCol = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                    textCol.setLayoutParams(lpCol);

                    TextView title = new TextView(activity);
                    title.setText(item.namePt);
                    title.setTextColor(Color.rgb(230, 230, 230));
                    title.setTextSize(13f);
                    title.setTypeface(Typeface.DEFAULT_BOLD);
                    textCol.addView(title);

                    TextView subText = new TextView(activity);
                    subText.setText(item.nameEn + " • " + item.roles);
                    subText.setTextColor(Color.rgb(150, 150, 160));
                    subText.setTextSize(11f);
                    textCol.addView(subText);

                    row.addView(textCol);
                    itemsContainer.addView(row);

                    // Divisor sutil
                    View div = new View(activity);
                    div.setBackgroundColor(Color.rgb(35, 35, 42));
                    div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    itemsContainer.addView(div);
                }
            }
        };

        // Criação das abas de filtro
        for (int i = 0; i < phases.length; i++) {
            final int idx = i;
            final TextView fBtn = new TextView(activity);
            fBtn.setText(phaseLabels[i]);
            fBtn.setTextSize(11f);
            fBtn.setTypeface(Typeface.DEFAULT_BOLD);
            fBtn.setPadding(pad12, pad8, pad12, pad8);
            if (i == 0) {
                fBtn.setBackgroundColor(Color.rgb(212, 175, 55));
                fBtn.setTextColor(Color.BLACK);
            } else {
                fBtn.setBackgroundColor(Color.rgb(32, 32, 40));
                fBtn.setTextColor(Color.rgb(200, 200, 210));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, pad8, pad8);
            fBtn.setLayoutParams(lp);

            fBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int j = 0; j < filterButtons.length; j++) {
                        if (j == idx) {
                            filterButtons[j].setBackgroundColor(Color.rgb(212, 175, 55));
                            filterButtons[j].setTextColor(Color.BLACK);
                        } else {
                            filterButtons[j].setBackgroundColor(Color.rgb(32, 32, 40));
                            filterButtons[j].setTextColor(Color.rgb(200, 200, 210));
                        }
                    }
                    try {
                        java.lang.reflect.Field field = populateItems.getClass().getDeclaredField("selectedPhase");
                        field.setAccessible(true);
                        field.set(populateItems, phases[idx]);
                    } catch (Exception ignored) {}
                    populateItems.run();
                }
            });
            filterButtons[i] = fBtn;
            filterBar.addView(fBtn);
        }
        hScroll.addView(filterBar);
        root.addView(hScroll);

        // Scroll de Itens
        ScrollView scroll = new ScrollView(activity);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (400 * density)));
        scroll.addView(itemsContainer);
        root.addView(scroll);

        // Preenche inicialmente
        populateItems.run();

        // Diálogo Principal
        final AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle("📋 Checklist do Turno do Reino")
            .setView(root)
            .setNeutralButton("REINICIAR CHECKLIST", null)
            .setPositiveButton("FECHAR", null)
            .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(activity)
                            .setTitle("Reiniciar Checklist?")
                            .setMessage("Isso desmarcará todas as atividades salvas para iniciar um novo Turno do Reino.")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("REINICIAR", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface confirmDialog, int which) {
                                    SharedPreferences.Editor edit = prefs.edit();
                                    for (ChecklistItem it : ITEMS) {
                                        edit.remove("chk_" + it.id);
                                    }
                                    edit.apply();
                                    populateItems.run();
                                }
                            })
                            .show();
                    }
                });
            }
        });

        dialog.show();
    }

    /**
     * Exibe a explicação detalhada de um tópico específico ao clicar no ícone [i].
     */
    public static void showItemDetailsDialog(Context context, ChecklistItem item) {
        if (context == null || item == null) return;

        float density = context.getResources().getDisplayMetrics().density;
        int pad16 = (int) (16 * density);
        int pad12 = (int) (12 * density);
        int pad8 = (int) (8 * density);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad16, pad8, pad16, pad16);

        // Título e Nome em Inglês
        TextView title = new TextView(context);
        title.setText(item.namePt + "\n(" + item.nameEn + ")");
        title.setTextSize(16f);
        title.setTextColor(Color.rgb(212, 175, 55));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(title);

        // Metadados
        TextView meta = new TextView(context);
        meta.setText("Fase: " + item.phaseLabel + " • Requisito: " + item.requirement + "\nFrequência: " + item.frequency);
        meta.setTextSize(12f);
        meta.setTextColor(Color.rgb(170, 170, 180));
        meta.setPadding(0, pad8, 0, pad8);
        layout.addView(meta);

        // Líderes
        TextView roles = new TextView(context);
        roles.setText("👑 Líderes Autorizados:\n" + item.roles);
        roles.setTextSize(12f);
        roles.setTextColor(Color.rgb(100, 200, 130)); // Green tint
        roles.setPadding(0, 0, 0, pad8);
        layout.addView(roles);

        // Descrição Narrativa / Breve Explicação
        TextView descHeader = new TextView(context);
        descHeader.setText("📖 O que é esta atividade:");
        descHeader.setTextSize(13f);
        descHeader.setTextColor(Color.rgb(220, 220, 220));
        descHeader.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(descHeader);

        TextView desc = new TextView(context);
        desc.setText(item.brief);
        desc.setTextSize(13f);
        desc.setTextColor(Color.rgb(210, 210, 210));
        desc.setPadding(0, 2, 0, pad12);
        layout.addView(desc);

        // Mecânica e Regras Oficiais
        TextView mechHeader = new TextView(context);
        mechHeader.setText("⚙️ Regras & Resolução (PF2e Kingmaker):");
        mechHeader.setTextSize(13f);
        mechHeader.setTextColor(Color.rgb(220, 220, 220));
        mechHeader.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(mechHeader);

        TextView mech = new TextView(context);
        mech.setText(item.mechanics);
        mech.setTextSize(12f);
        mech.setTextColor(Color.rgb(180, 180, 190));
        layout.addView(mech);

        ScrollView s = new ScrollView(context);
        s.addView(layout);

        new AlertDialog.Builder(context)
            .setView(s)
            .setPositiveButton("ENTENDIDO", null)
            .show();
    }
}
