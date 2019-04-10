package com.wix.vivaLaVita.database.dao

trait Identifiable[K, V] {
  def getId(value: V): K
}