import React from 'react';
import { Route } from 'lucide-react';
import { StoryChoice } from '../types';

interface ControlsProps {
  choices: StoryChoice[];
  onChoose: (choice: StoryChoice) => void;
  disabled: boolean;
}

export const Controls: React.FC<ControlsProps> = ({ choices, onChoose, disabled }) => (
  <div className="flex w-full flex-col gap-3 font-serif">
    {choices.map((choice, index) => (
      <button
        key={choice.id}
        onClick={() => onChoose({ ...choice, requiresTest: false, test: undefined })}
        disabled={disabled}
        className={`group relative w-full min-w-0 rounded-md border-2 p-3 text-left text-sm shadow-sm transition-all duration-200 ${
          disabled
            ? 'cursor-not-allowed border-slate-800/70 text-slate-700 opacity-50'
            : 'cursor-pointer border-slate-700/80 bg-slate-950/55 text-slate-200 hover:border-blue-600 hover:bg-blue-950/20 hover:text-white active:scale-[0.99]'
        }`}
      >
        <div className="mb-1.5 flex items-center gap-1.5 font-serif text-[10px] font-semibold tracking-wider text-slate-500">
          <Route className="h-3 w-3" /> SUGESTÃO {index + 1}
        </div>
        <span className="block break-words text-sm leading-relaxed [overflow-wrap:anywhere]">{choice.text}</span>
      </button>
    ))}
  </div>
);
