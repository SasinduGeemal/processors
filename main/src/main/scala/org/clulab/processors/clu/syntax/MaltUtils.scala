package org.clulab.processors.clu.syntax

import org.clulab.processors.{Processor, Sentence}
import org.clulab.struct.{DirectedGraph, Edge}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
  * Utils necessary for malt parsing
  * User: mihais
  * Date: 8/6/17
  */
object MaltUtils {
  def sentenceToConllx(sentence:Sentence): Array[String] = {
    // tokens stores the tokens in the input format expected by malt (CoNLL-X)
    val inputTokens = new Array[String](sentence.words.length)

    //println(s"WORDS: ${sentence.words.mkString(", ")}")
    //println(s"LEMMAS: ${sentence.lemmas.get.mkString(", ")}")
    //println(s"TAGS: ${sentence.tags.get.mkString(", ")}")
    for(i <- inputTokens.indices) {
      inputTokens(i) = s"${i + 1}\t${sentence.words(i)}\t${sentence.lemmas.get(i)}\t${sentence.tags.get(i)}\t${sentence.tags.get(i)}\t_"
    }

    inputTokens
  }

  def conllxToDirectedGraph(tokens:Array[String], internStrings:Boolean): DirectedGraph[String] = {
    val edgeBuffer = new ListBuffer[Edge[String]]
    val roots = new mutable.HashSet[Int]
    for(o <- tokens) {
      //println(o)
      val tokens = o.split("\\s+")
      if(tokens.length < 8)
        throw new RuntimeException("ERROR: Invalid malt output line: " + o)
      // malt indexes tokens from 1; we index from 0
      val modifier = tokens(0).toInt - 1
      val head = tokens(6).toInt - 1
      val label = tokens(7).toLowerCase

      // sometimes malt generates dependencies from root with a different label than "root"
      // not sure why this happens, but let's manage this: create a root node in these cases
      if(head == -1) {
        roots += modifier
      } else {
        edgeBuffer += Edge(source = head, destination = modifier, relation = in(label, internStrings))
      }
    }

    new DirectedGraph[String](edgeBuffer.toList, roots.toSet)
  }

  def directedGraphToConllx(dg:DirectedGraph[String], inputTokens: Array[String]):Array[String] = {

    // dependency map from modifier to (head, label)
    // we must have exactly 1 dependency for each modifier
    val depMap = new mutable.HashMap[Int, (Int, String)]()
    for(edge <- dg.allEdges) {
      val head = edge._1 + 1
      val mod = edge._2 + 2
      val label = edge._3
      depMap += mod -> (head, label)
    }
    for(root <- dg.roots) {
      val head = 0
      val mod = root
      val label = "root"
      depMap += mod -> (head, label)
    }

    // create CoNLL-X output including dependencies
    val conllxDeps = new ArrayBuffer[String]
    for(it <- inputTokens) {
      val bits = it.split("\\s+")
      val index = bits(0)
      val word = bits(1)
      val lemma = bits(2)
      val pos1 = bits(3)
      val pos2 = bits(4)
      val dep = depMap.get(index.toInt)
      assert(dep.nonEmpty)
      val head = dep.get._1
      val label = dep.get._2
      val token = s"$index\t$word\t$lemma\t$pos1\t$pos2\t_\t$head\t$label\t_\t_"
      conllxDeps += token
    }

    conllxDeps.toArray
  }

  def in(s:String, internStrings:Boolean):String = {
    if (internStrings) Processor.internString(s)
    else s
  }

  val FORWARD_NIVREEAGER_MODEL_NAME = "org/clulab/processors/clu/en-forward-nivreeager.mco"
  val FORWARD_NIVRESTANDARD_MODEL_NAME = "org/clulab/processors/clu/en-forward-nivrestandard.mco"
  val BACKWARD_NIVREEAGER_MODEL_NAME = "org/clulab/processors/clu/en-backward-nivreeager.mco"
  val BACKWARD_NIVRESTANDARD_MODEL_NAME = "org/clulab/processors/clu/en-backward-nivrestandard.mco"

  val DEFAULT_FORWARD_MODEL_NAME = FORWARD_NIVREEAGER_MODEL_NAME

  val DEFAULT_ENSEMBLE_MODELS = List(
    FORWARD_NIVREEAGER_MODEL_NAME,
    //FORWARD_NIVRESTANDARD_MODEL_NAME,
    //BACKWARD_NIVREEAGER_MODEL_NAME,
    BACKWARD_NIVRESTANDARD_MODEL_NAME
  )

}
