"""Keep actual completed construction payments per physical instance."""
def payment_repository(s):
 anchor='effects.put("official_build_effects",buildEffects);'
 assert s.count(anchor)==1
 s=s.replace(anchor,anchor+'''
                JSONObject payment=buildOptions.optJSONObject("construction_payment");
                if(payment==null)payment=new JSONObject().put("resource_points",prepaid?0:rpCost)
                    .put("lumber",prepaid?0:lumberCost).put("stone",prepaid?0:stoneCost)
                    .put("ore",prepaid?0:oreCost).put("luxuries",prepaid?0:luxuriesCost)
                    .put("authority",prepaid?"gift":"paid");
                effects.put("construction_payments",KingmakerStructureSemantics.appendedConstructionPayment(existing,payment));
''')
 anchor='JSONObject completed=buildStructure(townKey,structureKey,notes,true,buildOptions);'
 assert s.count(anchor)==1
 s=s.replace(anchor,'''boolean critical="CRITICAL_SUCCESS".equals(degree);
                    buildOptions.put("construction_payment",new JSONObject().put("resource_points",rp)
                        .put("lumber",lumber-(critical?lumber/2:0)).put("stone",stone-(critical?stone/2:0))
                        .put("ore",ore-(critical?ore/2:0)).put("luxuries",luxuries-(critical?luxuries/2:0))
                        .put("authority","paid").put("resumed_unfinished",resume));
                    '''+anchor)
 return s
