import test from 'node:test';
import assert from 'node:assert/strict';
import { STARTER_GENERAL_GEAR_REFERENCE, equipmentCatalog, generalGearCatalogEntry } from '../app/src/main/assets/vtt/js/rules/equipmentCatalog.mjs';

const EXPECTED = [
  ['Barra luminosa',10],['Comunicador',25],['Electrobinoculares',250],['Escáner manual',100],['Escáner multiuso',500],
  ['Ganzúa electrónica',1000],['Grilletes',25],['Inyector de estimulantes',25],['Juego de herramientas',350],['Macrobinoculares',75],
  ['Mando de retención',35],['Mascarilla respiratoria',25],['Material para disfrazarse',100],['Medpac',400],['Medpac de emergencia',100],
  ['Mochila',50],['Módulo de datos',75],['Parche de reparaciones de emergencia',25],['Raciones',5],['Traje espacial',100],
  ['Unidad de alimentación o recarga de munición',25]
];

test('Tabla 5-3 da Caja de Inicio fornece exatamente 21 itens gerais com preço',()=>{
  assert.equal(STARTER_GENERAL_GEAR_REFERENCE.length,21);
  assert.equal(new Set(STARTER_GENERAL_GEAR_REFERENCE.map(x=>x.id)).size,21);
  assert.deepEqual(STARTER_GENERAL_GEAR_REFERENCE.map(x=>[x.sourceNameEs,x.price]),EXPECTED);
});

test('Tabela 5-3 não autoriza inventar raridade nem restrição',()=>{
  for(const item of STARTER_GENERAL_GEAR_REFERENCE){
    assert.equal(item.rarity,null);
    assert.equal(item.restricted,null);
    assert.equal(item.source.book,'La Era de la Rebelión - Libro de reglas');
    assert.equal(item.source.pdfPage,36);
    assert.equal(item.source.section,'Tabla 5-3: Equipo general');
  }
});

test('referência geral entra no catálogo como cópia e preserva autoridade imutável',()=>{
  const c=equipmentCatalog();
  assert.ok(c.generalGear.length>=21);
  c.generalGear[0].name='alterado';
  assert.notEqual(STARTER_GENERAL_GEAR_REFERENCE[0].name,'alterado');
  assert.equal(generalGearCatalogEntry('medpac').price,400);
  assert.equal(generalGearCatalogEntry('missing'),null);
});
