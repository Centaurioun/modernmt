import argparse

import torch

from onmt import MMTDecoder
from onmt.Translator import Translator

parser = argparse.ArgumentParser(description='train.py')

# parser.add_argument('-train_from_state_dict', default='', type=str,
#                     help="""If training from a checkpoint then this is the
#                     path to the pretrained model's state_dict.""")


class OpenNMTDecoder(MMTDecoder):
    def __init__(self, model_path, gpu_index=-1):
        MMTDecoder.__init__(self, model_path)
        # TODO: stub implementation

        # TODO: how to create the opt object?
        ## Assuming that model_path is an actual model (and not is )
        checkpoint_path = model_path

        ###opt = parser.parse_args(args=parameters)
        opt = parser.parse_args(args="")
        opt.model = checkpoint_path
        opt.batch_size = 1
        opt.beam_size = 3
        opt.max_sent_length = 5
        opt.n_best = 3
        opt.replace_unk = False
        opt.verbose = False
        opt.tuning_epochs = 30

        opt.gpu = gpu_index
        if opt.gpu > -1:
            opt.cuda = True
        else:
            opt.cuda = False

        opt.seed = 3435
        # Sets the seed for generating random numbers
        if (opt.seed >= 0):
            torch.manual_seed(opt.seed)

        self.translator = Translator(opt)

    def translate(self, text, suggestions=None):
        # TODO: stub implementation

        srcBatch = []

        srcBatch.append(text)

        if len(suggestions) == 0:
            predBatch, predScore, goldScore = self.translator.translate(srcBatch, None)
        else:
            predBatch, predScore, goldScore = self.translator.translateWithAdaptation(srcBatch, None, suggestions)

        output = predBatch[0][0]

        # print of the nbest for each sentence of the batch
        for b in range(len(predBatch)):
            for n in range(len(predBatch[b])):
                print "def OpenNMTDecoder::translate(self, text, suggestions=None) predScore[b][n]:", repr(predScore[b][n]), " predBatch[b][n]:", repr(predBatch[b][n])

        return output

    def close(self):
        pass
