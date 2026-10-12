from copy import deepcopy


class InventoryEngine:
    """Canonical inventory authority; UI drag/drop only emits commands.

    The engine understands generic containers and delegates capacity semantics to
    the active SystemModule inventory model. The UI never computes legality.
    """
    def _inventory(self,state):
        inv=state.player_state.setdefault('inventory',{})
        if not isinstance(inv,dict): raise ValueError('invalid_inventory_state')
        containers=inv.setdefault('containers',{})
        if not isinstance(containers,dict): raise ValueError('invalid_inventory_containers')
        items=inv.setdefault('items',{})
        if not isinstance(items,dict): raise ValueError('invalid_inventory_items')
        return inv,containers,items

    def _item_slots(self,items,item_id):
        item=items.get(item_id,{})
        if not isinstance(item,dict): raise ValueError('invalid_inventory_item')
        slots=item.get('slots',item.get('size',1))
        if not isinstance(slots,int) or isinstance(slots,bool) or slots<1: raise ValueError('invalid_inventory_item_slots')
        return slots

    def _container_slot_capacity(self,inv,module,container_id):
        explicit=(inv.get('capacity') or {}).get(container_id) if isinstance(inv.get('capacity',{}),dict) else None
        if explicit is not None:
            if not isinstance(explicit,int) or isinstance(explicit,bool) or explicit<0: raise ValueError('invalid_inventory_capacity')
            return explicit
        model=dict(module.inventory_model)
        zones=model.get('zones',{}) if isinstance(model.get('zones',{}),dict) else {}
        cap=zones.get(container_id)
        return int(cap) if isinstance(cap,int) and not isinstance(cap,bool) and cap>=0 else None

    def _used_slots(self,items,item_ids):
        return sum(self._item_slots(items,item_id) for item_id in item_ids)

    def _weight(self,items,item_ids):
        total=0.0
        for item_id in item_ids:
            item=items.get(item_id,{})
            if not isinstance(item,dict): raise ValueError('invalid_inventory_item')
            weight=item.get('weight',0)
            qty=item.get('quantity',1)
            if not isinstance(weight,(int,float)) or isinstance(weight,bool) or weight<0: raise ValueError('invalid_inventory_item_weight')
            if not isinstance(qty,(int,float)) or isinstance(qty,bool) or qty<0: raise ValueError('invalid_inventory_item_quantity')
            total+=float(weight)*float(qty)
        return total

    def validate_move(self,state,command,module):
        required={'item_id','from_container','to_container'}
        if not isinstance(command,dict) or not required<=set(command): raise ValueError('invalid_move_item_command')
        inv,containers,items=self._inventory(state)
        src=containers.get(command['from_container']); dst=containers.get(command['to_container'])
        if not isinstance(src,list) or not isinstance(dst,list): raise ValueError('inventory_container_not_found')
        item_id=command['item_id']
        if item_id not in src: raise ValueError('inventory_item_not_found')
        if command['from_container']==command['to_container']: return True
        capacity=self._container_slot_capacity(inv,module,command['to_container'])
        if capacity is not None and self._used_slots(items,dst)+self._item_slots(items,item_id)>capacity:
            raise ValueError('inventory_capacity_exceeded')
        model=dict(module.inventory_model)
        if model.get('model') in {'weight','encumbrance'}:
            max_weight=inv.get('max_weight')
            if max_weight is not None:
                if not isinstance(max_weight,(int,float)) or isinstance(max_weight,bool) or max_weight<0: raise ValueError('invalid_inventory_max_weight')
                all_ids=[]
                for cid,ids in containers.items():
                    if not isinstance(ids,list): raise ValueError('invalid_inventory_container')
                    all_ids.extend(ids)
                # Moving does not change total carried weight, but this prevents an
                # already-invalid host projection from bypassing the authority boundary.
                if self._weight(items,all_ids)>float(max_weight): raise ValueError('inventory_weight_exceeded')
        return True

    def move(self,state,command,module,*,emit_event=True):
        self.validate_move(state,command,module)
        inv,containers,_=self._inventory(state)
        src=containers[command['from_container']]; dst=containers[command['to_container']]
        item_id=command['item_id']
        if command['from_container']!=command['to_container']:
            src.remove(item_id); dst.append(item_id)
        event={'event_id':f"inventory:{state.state_version}:{item_id}",'type':'item_moved','request_id':command.get('request_id','inventory'),'tick':state.tick,'state_version':state.state_version,'payload':deepcopy(command)}
        if emit_event: state.event_log.append(event)
        return event

    def consume(self,state,command,module,*,emit_event=True):
        required={'item_id','quantity'}
        if not isinstance(command,dict) or not required<=set(command): raise ValueError('invalid_consume_item_command')
        quantity=command['quantity']
        if not isinstance(quantity,(int,float)) or isinstance(quantity,bool) or quantity<=0: raise ValueError('invalid_inventory_item_quantity')
        inv,containers,items=self._inventory(state)
        item=items.get(command['item_id'])
        if not isinstance(item,dict): raise ValueError('inventory_item_not_found')
        old=item.get('quantity',1)
        if not isinstance(old,(int,float)) or isinstance(old,bool) or old<quantity: raise ValueError('inventory_insufficient_quantity')
        item['quantity']=old-quantity
        if item['quantity']==0:
            for ids in containers.values():
                if isinstance(ids,list): ids[:]=[x for x in ids if x!=command['item_id']]
        event={'event_id':f"inventory:{state.state_version}:consume:{command['item_id']}",'type':'item_consumed','request_id':command.get('request_id','inventory'),'tick':state.tick,'state_version':state.state_version,'payload':deepcopy(command)}
        if emit_event: state.event_log.append(event)
        return event


class QuickActionResolver:
    def resolve(self,state,module,target=None):
        actions=[]
        for action_id,definition in module.action_definitions.items(): actions.append({'id':action_id,'source':'system','definition':deepcopy(definition)})
        for item in (state.player_state.get('equipped') or []):
            if isinstance(item,dict):
                for action in item.get('actions',[]): actions.append({'id':action,'source':'equipment','item_id':item.get('id')})
        conditions=set(state.player_state.get('conditions') or [])
        filtered=[]
        for action in actions:
            definition=action.get('definition') or {}
            blocked=set(definition.get('blocked_by_conditions') or []) if isinstance(definition,dict) else set()
            if conditions & blocked: continue
            filtered.append(action)
        if target: filtered=[{**a,'target':target} for a in filtered]
        return filtered
