# TrUDucer

this is a fork of [this Gitlab Repository by Felix Henning](https://gitlab.com/nats/TrUDucer.git).

<b>Tr</b>ansforming to <b>U</b>niversal <b>D</b>ependencies with
transd<b>ucer</b>s.

A tree transducer based conversion tool for dependency trees.  Convert
dependency trees given as CoNLL files from a source annotation schema
to a new target annotation schema.

For in depth information take a look
at [the accompanying website](https://nats.gitlab.io/truducer)
and [the accompanying paper](http://nbn-resolving.de/urn:nbn:de:gbv:18-228-7-2303).

## Features

- Convert individual trees 
- Convert a treebank given as a directory
- Visualize a conversion process of a tree

- Compare trees to test precision and check coverage on converted trees

## Usage

Dependency Trees in the CoNLL file format are converted with a a
top-down tree transducer formalism.  The tree transducer rules are
specified in a rulefile, a sample of a rulefile with example rules is
included in this repository
([sample_rules_hdt.tud](sample_rules_hdt.tud)).

Usage information taken from the help of the software itself:

```
usage: TrUDucer [-h] {conv,convall,compare,coverage,show} ...

TrUDucer - Transforming to Universal Dependencies with transducers.

Tree transducer based dependency tree annotation schema conversion.  A
single CoNLL file or a whole directory can be converted based on given
rule file; can compare directories, useful for testing the correctness
of a rulefile with previously annotated trees.Test the coverage of a
rulefile with 'coverage' or look at a conversion process in detail
with 'show'.

For the compare and coverage subcommand it is assumed that the source
dependency relations are in CAPS, while the UD labels are in all
lowercase.  Otherwise no assumptions about the labels are made.

positional arguments:
  {conv,convall,compare,coverage,show}
                         The various subcommands.
    conv                 Convert a single tree given by a CoNLL file.
    convall              Convert a whole directory of CoNLL files.
    compare              Compare two directories of CoNLL files.
    coverage             Check how many dependency relations are converted; in a single directory.
    show                 Show the conversion process of a single tree step by step in a GUI.

optional arguments:
  -h, --help             show this help message and exit
```

## Data

We use TrUDucer to convert the Hamburg Dependency Treebank.  As of
now, [a set of manually converted sentences](https://gitlab.com/nats/TrUDucer_experiment_data)
is available which we used for developing and evaluating the example rule file.

## License

This code was written by Felix Hennig.

The software is licensed under the GNU General Public License v3.
