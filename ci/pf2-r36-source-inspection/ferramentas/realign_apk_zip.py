import zipfile,sys,os,struct
src,dst=sys.argv[1],sys.argv[2]
with zipfile.ZipFile(src,'r') as zin, open(dst,'wb') as raw:
  with zipfile.ZipFile(raw,'w',allowZip64=True) as zout:
    for item in zin.infolist():
      n=item.filename; data=zin.read(item)
      zi=zipfile.ZipInfo(n,date_time=(1981,1,1,1,1,0))
      zi.compress_type=item.compress_type; zi.comment=item.comment; zi.internal_attr=item.internal_attr; zi.external_attr=item.external_attr; zi.create_system=item.create_system; zi.flag_bits=item.flag_bits & ~0x08
      if zi.compress_type==zipfile.ZIP_STORED and not n.endswith('/'):
        nb=n.encode('utf-8'); base=zout.fp.tell()+30+len(nb); rem=base%4
        if rem:
          total=(4-rem)%4
          if total<4: total+=4
          zi.extra=struct.pack('<HH',0xCAFE,total-4)+b'\0'*(total-4)
      zout.writestr(zi,data,compress_type=zi.compress_type,compresslevel=9 if zi.compress_type==zipfile.ZIP_DEFLATED else None)
