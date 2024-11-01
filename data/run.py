import os
import GPUtil
import sys
import os

os.environ["CUDA_VISIBLE_DEVICES"] = "5"

GPUs = GPUtil.getGPUs()
for gpu in GPUs:
    print(f'GPU {gpu.id} has {gpu.load*100}% utilization')

# Add the TensorFlow directory to the system path
sys.path.append('TensorFlow')

from src_tokenizer.tokenization_morp2 import FullTokenizer

# Set the correct path for the vocab file
vocab_file = 'TensorFlow/vocab.korean_morp.list'

# Load the pre-trained KorBERT Morphology tokenizer
tokenizer = FullTokenizer(vocab_file=vocab_file, do_lower_case=False)

# Example input text and its morphological analysis
text = "ETRI/SL 에서/JKB 한국어/NNP BERT/SL 언어/NNG 모델/NNG 을/JKO 배포/NNG 하/XSV 었/EP 다/EF ./SF"

# Tokenize the input using the tokenizer
tokens = tokenizer.tokenize(text)
print("Tokens:", tokens)

# Tokenize the input using the morphological analysis
input_ids = tokenizer.convert_tokens_to_ids(tokens)
print("Token IDs:", input_ids)

# Convert token IDs back to tokens
reconverted_tokens = tokenizer.convert_ids_to_tokens(input_ids)
print("Reconverted Tokens:", reconverted_tokens)