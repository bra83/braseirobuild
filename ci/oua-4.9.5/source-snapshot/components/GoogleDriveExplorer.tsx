import React, { useState, useEffect, useRef } from 'react';
import { 
  Folder, File, ArrowLeft, Search, Loader2, Check, Download, 
  AlertCircle, FolderOpen, RefreshCw, X, Shield, Lock, FileText,
  Archive, HelpCircle
} from 'lucide-react';
import { googleSignIn, getAccessToken, logout } from '../lib/auth';
import { downloadDriveFile } from '../lib/drive';

interface GoogleDriveFile {
  id: string;
  name: string;
  mimeType: string;
  size?: string;
  modifiedTime?: string;
}

interface GoogleDriveExplorerProps {
  isOpen: boolean;
  onClose: () => void;
  onFileSelected: (blob: Blob, fileName: string, mimeType: string) => Promise<void>;
  allowedExtensions: string[]; // e.g. ['pdf', 'txt', 'md'] or ['json', 'zip']
  title: string;
}

export const GoogleDriveExplorer: React.FC<GoogleDriveExplorerProps> = ({
  isOpen,
  onClose,
  onFileSelected,
  allowedExtensions,
  title
}) => {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<any>(null);
  const [files, setFiles] = useState<GoogleDriveFile[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentFolderId, setCurrentFolderId] = useState<string | null>(null);
  const [folderHistory, setFolderHistory] = useState<Array<{ id: string | null; name: string }>>([
    { id: null, name: 'Raiz' }
  ]);
  const [downloadingFileId, setDownloadingFileId] = useState<string | null>(null);
  const [downloadProgress, setDownloadProgress] = useState<string | null>(null);

  // Load active token on mount / open
  useEffect(() => {
    if (isOpen) {
      checkAuth();
    }
  }, [isOpen]);

  const checkAuth = async () => {
    try {
      const activeToken = await getAccessToken();
      if (activeToken) {
        setToken(activeToken);
        // Load root folder
        loadFolderContents(null, activeToken);
      } else {
        setToken(null);
      }
    } catch (err) {
      console.error("Auth check failed:", err);
    }
  };

  const handleLogin = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await googleSignIn();
      if (result) {
        setToken(result.accessToken);
        setUser(result.user);
        await loadFolderContents(null, result.accessToken);
      }
    } catch (err: any) {
      console.error("Login failed:", err);
      setError("Falha ao autenticar com o Google Drive. Verifique se as permissões foram concedidas.");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
      setToken(null);
      setFiles([]);
      setCurrentFolderId(null);
      setFolderHistory([{ id: null, name: 'Raiz' }]);
    } catch (err) {
      console.error(err);
    }
  };

  const loadFolderContents = async (folderId: string | null, activeToken?: string) => {
    const apiToken = activeToken || token;
    if (!apiToken) return;

    setLoading(true);
    setError(null);

    try {
      // Build search query matching folders or allowed extensions
      let q = "trashed = false";
      
      if (folderId) {
        q += ` and '${folderId}' in parents`;
      } else {
        q += ` and 'root' in parents`;
      }

      // Add file filters for search querying or folder listings
      // We list folders always, and files based on allowed types
      const mimeTypes: string[] = allowedExtensions.map(ext => {
        if (ext === 'pdf') return "mimeType = 'application/pdf'";
        if (ext === 'txt') return "mimeType = 'text/plain'";
        if (ext === 'md') return "mimeType = 'text/markdown'";
        if (ext === 'json') return "mimeType = 'application/json'";
        if (ext === 'zip') return "mimeType = 'application/zip' or mimeType = 'application/x-zip-compressed' or name contains '.zip'";
        return '';
      }).filter(Boolean);

      // Google Docs só podem ser exportados quando a tela aceita texto.
      const acceptsTextExport = allowedExtensions.includes('txt') || allowedExtensions.includes('md');
      if (acceptsTextExport) {
        mimeTypes.push("mimeType = 'application/vnd.google-apps.document'");
      }

      const fileFilter = mimeTypes.length > 0 ? `(${mimeTypes.join(' or ')})` : '';
      
      let finalQuery = q;
      if (fileFilter) {
        finalQuery += ` and (mimeType = 'application/vnd.google-apps.folder' or ${fileFilter})`;
      } else {
        finalQuery += " and mimeType = 'application/vnd.google-apps.folder'";
      }

      const url = `https://www.googleapis.com/drive/v3/files?q=${encodeURIComponent(finalQuery)}&fields=files(id,name,mimeType,size,modifiedTime)&pageSize=1000&orderBy=folder,name`;
      
      const response = await fetch(url, {
        headers: { Authorization: `Bearer ${apiToken}` }
      });

      if (!response.ok) {
        const errData = await response.json().catch(() => ({}));
        if (response.status === 401) {
          // Token expired
          setToken(null);
          throw new Error("Sessão expirada. Por favor, conecte-se novamente.");
        }
        throw new Error(errData.error?.message || "Erro ao carregar arquivos do Drive.");
      }

      const data = await response.json();
      setFiles(data.files || []);
      setCurrentFolderId(folderId);
    } catch (err: any) {
      console.error("Drive file listing failed:", err);
      setError(err.message || "Não foi possível listar os arquivos do seu Google Drive.");
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;

    if (!searchQuery.trim()) {
      loadFolderContents(currentFolderId);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      // Search files across all folders matching extensions
      const mimeTypes: string[] = allowedExtensions.map(ext => {
        if (ext === 'pdf') return "mimeType = 'application/pdf'";
        if (ext === 'txt') return "mimeType = 'text/plain'";
        if (ext === 'md') return "mimeType = 'text/markdown'";
        if (ext === 'json') return "mimeType = 'application/json'";
        if (ext === 'zip') return "mimeType = 'application/zip' or mimeType = 'application/x-zip-compressed' or name contains '.zip'";
        return '';
      }).filter(Boolean);

      const acceptsTextExport = allowedExtensions.includes('txt') || allowedExtensions.includes('md');
      if (acceptsTextExport) {
        mimeTypes.push("mimeType = 'application/vnd.google-apps.document'");
      }

      const fileFilter = mimeTypes.length > 0 ? `(${mimeTypes.join(' or ')})` : '';
      
      let q = `name contains '${searchQuery.replace(/'/g, "\\'")}' and trashed = false`;
      if (fileFilter) {
        q += ` and ${fileFilter}`;
      }

      const url = `https://www.googleapis.com/drive/v3/files?q=${encodeURIComponent(q)}&fields=files(id,name,mimeType,size,modifiedTime)&pageSize=1000&orderBy=name`;

      const response = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` }
      });

      if (!response.ok) {
        throw new Error("Erro na busca de arquivos.");
      }

      const data = await response.json();
      setFiles(data.files || []);
    } catch (err: any) {
      console.error(err);
      setError("Falha ao realizar busca no Google Drive.");
    } finally {
      setLoading(false);
    }
  };

  const enterFolder = (folderId: string, folderName: string) => {
    const newHistory = [...folderHistory, { id: folderId, name: folderName }];
    setFolderHistory(newHistory);
    setSearchQuery('');
    loadFolderContents(folderId);
  };

  const navigateToHistoryIndex = (index: number) => {
    const target = folderHistory[index];
    const newHistory = folderHistory.slice(0, index + 1);
    setFolderHistory(newHistory);
    setSearchQuery('');
    loadFolderContents(target.id);
  };

  const handleBack = () => {
    if (folderHistory.length <= 1) return;
    navigateToHistoryIndex(folderHistory.length - 2);
  };

  const downloadAndProcessFile = async (file: GoogleDriveFile) => {
    if (!token) return;
    setDownloadingFileId(file.id);
    setDownloadProgress("Iniciando download...");
    setError(null);

    try {
      setDownloadProgress(file.mimeType === 'application/vnd.google-apps.document'
        ? "Exportando documento do Google Docs..."
        : "Baixando arquivo...");
      // Usa o mesmo transporte resiliente da sincronização da pasta: timeout,
      // renovação de token e nova tentativa pública quando for permitido.
      const blob = await downloadDriveFile(file.id, file.mimeType);
      
      setDownloadProgress("Processando arquivo no sistema...");
      const finalName = file.mimeType === 'application/vnd.google-apps.document' && !file.name.endsWith('.txt') 
        ? `${file.name}.txt` 
        : file.name;

      const finalMime = file.mimeType === 'application/vnd.google-apps.document' 
        ? 'text/plain' 
        : file.mimeType;

      await onFileSelected(blob, finalName, finalMime);
      
      setDownloadProgress("Sucesso absoluto!");
      setTimeout(() => {
        onClose();
        setDownloadingFileId(null);
        setDownloadProgress(null);
      }, 800);
    } catch (err: any) {
      console.error("Failed downloading file:", err);
      setError(err.message || "Erro crítico ao baixar arquivo do Google Drive.");
      setDownloadingFileId(null);
      setDownloadProgress(null);
    }
  };

  const getFileIcon = (mimeType: string) => {
    if (mimeType === 'application/vnd.google-apps.folder') {
      return <Folder className="w-5 h-5 text-blue-500 fill-blue-500/20" />;
    }
    if (mimeType === 'application/pdf') {
      return <FileText className="w-5 h-5 text-red-500" />;
    }
    if (mimeType === 'text/plain' || mimeType === 'text/markdown' || mimeType === 'application/vnd.google-apps.document') {
      return <FileText className="w-5 h-5 text-blue-400" />;
    }
    if (mimeType === 'application/json') {
      return <FileText className="w-5 h-5 text-yellow-500" />;
    }
    if (mimeType.includes('zip') || mimeType.includes('compressed')) {
      return <Archive className="w-5 h-5 text-emerald-500" />;
    }
    return <File className="w-5 h-5 text-blue-700/60" />;
  };

  const formatSize = (bytes?: string) => {
    if (!bytes) return '-';
    const num = parseInt(bytes);
    if (isNaN(num)) return '-';
    if (num < 1024) return `${num} B`;
    if (num < 1048576) return `${(num / 1024).toFixed(1)} KB`;
    return `${(num / 1048576).toFixed(1)} MB`;
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-[100] flex items-center justify-center p-4 animate-fadeIn select-none">
      
      <div className="relative w-full max-w-2xl bg-zinc-950 border-2 border-blue-900 rounded-md shadow-[0_0_50px_rgba(0,0,0,0.8)] overflow-hidden font-serif flex flex-col h-[520px]">
        {/* Tolkien Gold Corners */}
        <div className="absolute top-2 left-2 w-3 h-3 border-l-2 border-t-2 border-blue-500/50"></div>
        <div className="absolute top-2 right-2 w-3 h-3 border-r-2 border-t-2 border-blue-500/50"></div>
        <div className="absolute bottom-2 left-2 w-3 h-3 border-l-2 border-b-2 border-blue-500/50"></div>
        <div className="absolute bottom-2 right-2 w-3 h-3 border-r-2 border-b-2 border-blue-500/50"></div>

        {/* Modal Header */}
        <div className="px-5 py-4 border-b border-blue-900/40 bg-[#090807] flex justify-between items-center shrink-0">
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-blue-500 shadow-[0_0_6px_#3b82f6] animate-pulse"></div>
            <h3 className="font-serif text-xs font-bold tracking-widest text-blue-500 uppercase">{title}</h3>
          </div>
          <button 
            onClick={onClose}
            className="p-1 text-blue-700 hover:text-blue-300 rounded cursor-pointer transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Content */}
        <div className="flex-1 overflow-hidden flex flex-col p-5">
          {!token ? (
            /* Unauthenticated State */
            <div className="flex-1 flex flex-col items-center justify-center text-center max-w-md mx-auto gap-5">
              <div className="w-16 h-16 rounded-full bg-blue-950/20 border border-blue-900/40 flex items-center justify-center text-blue-500 ambient-glow">
                <Shield className="w-8 h-8" />
              </div>
              <div>
                <h4 className="font-serif font-bold text-blue-400 text-sm tracking-wide">CONECTAR AO GOOGLE DRIVE</h4>
                <p className="text-xs text-blue-200/70 mt-2 leading-relaxed">
                  Para importar manuais e fichas de RPG diretamente do seu Drive, conecte sua conta do Google com segurança. O Braseiro respeita as diretrizes de privacidade e terá acesso apenas para leitura de arquivos autorizados.
                </p>
              </div>

              {loading ? (
                <div className="flex items-center gap-2 text-xs text-blue-400">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Conectando com segurança...</span>
                </div>
              ) : (
                <button
                  onClick={handleLogin}
                  className="px-5 py-3 bg-blue-950/20 hover:bg-blue-600/20 border-2 border-blue-600 text-blue-400 font-bold hover:text-blue-100 uppercase tracking-widest font-serif text-xs rounded cursor-pointer transition-all flex items-center gap-2"
                >
                  <Lock className="w-4 h-4 text-blue-500" />
                  AUTORIZAR ACESSO AO DRIVE
                </button>
              )}
            </div>
          ) : (
            /* Authenticated Explorer State */
            <div className="flex-1 flex flex-col overflow-hidden">
              {/* Toolbar */}
              <div className="flex flex-col sm:flex-row gap-3 pb-4 border-b border-blue-900/10 shrink-0">
                {/* Search Form */}
                <form onSubmit={handleSearch} className="flex-1 relative">
                  <Search className="w-3.5 h-3.5 text-blue-800 absolute left-2.5 top-2.5" />
                  <input
                    type="text"
                    placeholder="Buscar arquivos no Drive..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full pl-8 pr-16 py-1.5 bg-zinc-900 border border-blue-900/20 rounded text-xs text-blue-100 focus:outline-none focus:border-blue-700 font-sans"
                  />
                  {searchQuery && (
                    <button
                      type="button"
                      onClick={() => { setSearchQuery(''); loadFolderContents(currentFolderId); }}
                      className="absolute right-12 top-2 text-[10px] text-blue-600 hover:text-blue-300"
                    >
                      Limpar
                    </button>
                  )}
                  <button
                    type="submit"
                    className="absolute right-1 top-1.5 px-2 py-0.5 bg-blue-950/40 hover:bg-blue-800/40 text-[9px] text-blue-400 font-semibold font-serif rounded uppercase"
                  >
                    Ir
                  </button>
                </form>

                {/* Logout Button */}
                <div className="flex justify-between items-center sm:justify-end gap-3 text-[10px] text-blue-700">
                  <span className="truncate max-w-[120px] font-sans font-medium text-blue-500">Logado</span>
                  <button
                    onClick={handleLogout}
                    className="px-2 py-1 bg-zinc-900 border border-blue-950 hover:border-red-900/60 text-blue-700 hover:text-red-400 font-serif rounded cursor-pointer transition-colors"
                  >
                    Desconectar
                  </button>
                </div>
              </div>

              {/* Breadcrumbs / Folder Trail */}
              <div className="py-2.5 flex items-center gap-1.5 text-[10px] text-blue-500 font-serif font-bold uppercase overflow-x-auto no-scrollbar border-b border-blue-900/10 shrink-0">
                <button
                  onClick={handleBack}
                  disabled={folderHistory.length <= 1}
                  className="p-1 hover:bg-blue-950/20 disabled:opacity-35 disabled:hover:bg-transparent rounded cursor-pointer shrink-0"
                  title="Voltar"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                </button>
                
                <span className="text-blue-800">|</span>

                <div className="flex items-center gap-1 truncate font-sans">
                  {folderHistory.map((folder, index) => (
                    <React.Fragment key={index}>
                      {index > 0 && <span className="text-blue-800/50 mx-0.5 font-bold font-serif">/</span>}
                      <button
                        onClick={() => navigateToHistoryIndex(index)}
                        className={`hover:text-blue-300 truncate max-w-[100px] ${
                          index === folderHistory.length - 1 ? 'text-blue-300 font-bold' : 'text-blue-600'
                        }`}
                        style={{ cursor: index === folderHistory.length - 1 ? 'default' : 'pointer' }}
                        disabled={index === folderHistory.length - 1}
                      >
                        {folder.name}
                      </button>
                    </React.Fragment>
                  ))}
                </div>
              </div>

              {/* Status and Errors */}
              {error && (
                <div className="mt-3 p-2 bg-red-950/20 border border-red-900/30 text-red-400 text-xs rounded flex items-center gap-2 shrink-0">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              {/* Files Grid/List Container */}
              <div className="flex-1 overflow-y-auto scrollbar-thin mt-3 min-h-0">
                {loading && !downloadingFileId ? (
                  <div className="py-20 flex flex-col items-center justify-center gap-2 text-blue-700 text-xs">
                    <Loader2 className="w-6 h-6 animate-spin text-blue-500" />
                    <span className="font-serif uppercase font-bold tracking-wider animate-pulse">Percorrendo o Drive...</span>
                  </div>
                ) : files.length === 0 ? (
                  <div className="py-16 text-center text-blue-900/70 text-xs flex flex-col items-center justify-center gap-1.5">
                    <FolderOpen className="w-10 h-10 mb-1 opacity-20" />
                    <p className="font-serif font-bold">Nenhum arquivo ou pasta correspondente</p>
                    <p className="italic font-serif text-[11px]">Formatos aceitos: {allowedExtensions.join(', ').toUpperCase()}</p>
                  </div>
                ) : (
                  <div className="divide-y divide-blue-950/25">
                    {files.map((file) => {
                      const isFolder = file.mimeType === 'application/vnd.google-apps.folder';
                      const isDownloading = downloadingFileId === file.id;

                      return (
                        <div
                          key={file.id}
                          onClick={() => {
                            if (isFolder) {
                              enterFolder(file.id, file.name);
                            } else if (!downloadingFileId) {
                              downloadAndProcessFile(file);
                            }
                          }}
                          className={`group py-3 px-2.5 flex items-center justify-between gap-3 text-xs transition-colors rounded hover:bg-blue-950/10 cursor-pointer ${
                            isDownloading ? 'bg-blue-950/20 border-y border-blue-900/30' : ''
                          }`}
                        >
                          <div className="flex items-center gap-3 min-w-0">
                            <div className="shrink-0 transition-transform group-hover:scale-105 duration-150">
                              {getFileIcon(file.mimeType)}
                            </div>
                            <div className="min-w-0">
                              <span className="font-bold text-blue-200 group-hover:text-blue-400 block truncate transition-colors" title={file.name}>
                                {file.name}
                              </span>
                              <div className="flex items-center gap-2 text-[9px] text-blue-700/80 mt-0.5">
                                {!isFolder && <span>{formatSize(file.size)}</span>}
                                {!isFolder && <span>•</span>}
                                <span>Modificado em {file.modifiedTime ? new Date(file.modifiedTime).toLocaleDateString() : 'Desconhecido'}</span>
                                {file.mimeType === 'application/vnd.google-apps.document' && (
                                  <>
                                    <span>•</span>
                                    <span className="text-blue-500 font-semibold uppercase">Exportável para texto</span>
                                  </>
                                )}
                              </div>
                            </div>
                          </div>

                          {/* Action / State Column */}
                          <div className="shrink-0">
                            {isFolder ? (
                              <span className="text-[10px] text-blue-600 font-serif font-bold group-hover:text-blue-400 tracking-wide transition-colors uppercase">Abrir</span>
                            ) : isDownloading ? (
                              <div className="flex items-center gap-2 text-blue-400 font-semibold font-sans text-[10px]">
                                <Loader2 className="w-3 h-3 animate-spin text-blue-500" />
                                <span className="animate-pulse">{downloadProgress}</span>
                              </div>
                            ) : (
                              <button
                                disabled={!!downloadingFileId}
                                className="p-1.5 bg-zinc-900 border border-blue-950 hover:border-blue-900 text-blue-700 hover:text-blue-400 rounded cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
                                title="Importar este arquivo"
                              >
                                <Download className="w-3.5 h-3.5" />
                              </button>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Modal Footer Info */}
        {token && (
          <div className="px-5 py-3 border-t border-blue-900/40 bg-[#090807]/90 text-[10px] text-blue-700/80 font-sans flex items-center justify-between shrink-0">
            <div className="flex items-center gap-1">
              <Shield className="w-3 h-3 text-blue-700" />
              <span>Conexão criptografada diretamente com a API do Google Drive</span>
            </div>
            <div className="hidden sm:flex items-center gap-1.5">
              <HelpCircle className="w-3 h-3 text-blue-800" />
              <span className="italic">Clique nos arquivos para baixá-los instantaneamente.</span>
            </div>
          </div>
        )}

      </div>
    </div>
  );
};
