import React, { useState } from 'react';
import { Pencil } from 'lucide-react';
import { useVisualTheme } from '../lib/useVisualTheme';
import { themes } from '../lib/themes';

export const ThemeSelector: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const { currentTheme, previewTheme, setPreviewTheme, applyTheme, cancelPreview } = useVisualTheme();

  if (!isOpen) {
    return (
      <button
        onClick={() => setIsOpen(true)}
        className="p-1.5 bg-blue-950/30 hover:bg-blue-900/25 border border-blue-900/40 rounded text-blue-300 hover:text-blue-100 cursor-pointer transition-colors"
        title="Alterar tema visual"
        aria-label="Alterar tema visual do aplicativo"
      >
        <Pencil className="w-3.5 h-3.5" />
      </button>
    );
  }

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 p-4">
      <div className="bg-zinc-900 border border-blue-900 rounded-lg p-6 w-full max-w-lg shadow-2xl">
        <h2 className="text-xl font-bold text-blue-400 mb-4 font-serif">APARÊNCIA DO BRASEIRO</h2>
        <div className="grid grid-cols-1 gap-4 mb-6">
          {themes.map((theme) => {
            const isSelected = (previewTheme || currentTheme) === theme.id;
            return (
              <div
                key={theme.id}
                className={`border rounded p-4 ${isSelected ? 'border-blue-500 bg-blue-950/20' : 'border-blue-900 bg-zinc-950'} cursor-pointer hover:border-blue-500`}
                onClick={() => setPreviewTheme(theme.id)}
              >
                <h3 className="font-bold text-blue-300">{theme.name}</h3>
                <p className="text-sm text-blue-700">{theme.description}</p>
                {isSelected && <span className="text-xs text-blue-500 font-bold">Tema atual</span>}
              </div>
            );
          })}
        </div>
        <div className="flex justify-end gap-2">
          <button
            onClick={() => {
              cancelPreview();
              setIsOpen(false);
            }}
            className="px-4 py-2 border border-blue-900 rounded text-blue-700 hover:text-blue-500"
          >
            Cancelar
          </button>
          <button
            onClick={() => {
              applyTheme(previewTheme || currentTheme);
              setIsOpen(false);
            }}
            className="px-4 py-2 bg-blue-900 rounded text-white hover:bg-blue-800"
          >
            Aplicar
          </button>
        </div>
      </div>
    </div>
  );
};
