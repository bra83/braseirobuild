#include "llama.h"
#include <iostream>
#include <string>
#include <vector>
#include <iterator>
#include <memory>
// One isolated process, prompt exclusively on stdin, no logs or network.
static void silent(enum ggml_log_level,const char*,void*){}
int main(int argc,char** argv){
 if(argc!=2&&argc!=3)return 2;
 const int budget=argc==3&&std::string(argv[2])=="384"?384:192;
 if(argc==3&&budget!=384)return 2;
 std::string prompt((std::istreambuf_iterator<char>(std::cin)),{});if(prompt.empty()||prompt.size()>24000)return 3;
 llama_log_set(silent,nullptr);llama_backend_init();
 auto mp=llama_model_default_params();mp.n_gpu_layers=0;mp.use_mmap=true;
 std::unique_ptr<llama_model,decltype(&llama_model_free)> model(llama_model_load_from_file(argv[1],mp),llama_model_free);if(!model)return 4;
 const auto* vocab=llama_model_get_vocab(model.get());
 std::vector<llama_token> tokens(2048);
 int n=llama_tokenize(vocab,prompt.data(),(int)prompt.size(),tokens.data(),(int)tokens.size(),false,true);
 if(n<=0||n>1600)return 5;tokens.resize(n);
 auto cp=llama_context_default_params();cp.n_ctx=2048;cp.n_batch=512;cp.n_ubatch=256;cp.n_threads=2;cp.n_threads_batch=2;
 std::unique_ptr<llama_context,decltype(&llama_free)> ctx(llama_init_from_model(model.get(),cp),llama_free);if(!ctx)return 6;
 for(int i=0;i<n;i+=512){int count=std::min(512,n-i);if(llama_decode(ctx.get(),llama_batch_get_one(tokens.data()+i,count)))return 7;}
 std::unique_ptr<llama_sampler,decltype(&llama_sampler_free)> sampler(llama_sampler_init_greedy(),llama_sampler_free);if(!sampler)return 8;
 for(int i=0;i<budget;i++){llama_token t=llama_sampler_sample(sampler.get(),ctx.get(),-1);if(llama_vocab_is_eog(vocab,t))break;
 char b[1024];int k=llama_token_to_piece(vocab,t,b,sizeof b,0,false);if(k<0)return 9;if(k>0){std::cout.write(b,k);std::cout.flush();}
 if(llama_decode(ctx.get(),llama_batch_get_one(&t,1)))return 10;}
 return 0;
}
