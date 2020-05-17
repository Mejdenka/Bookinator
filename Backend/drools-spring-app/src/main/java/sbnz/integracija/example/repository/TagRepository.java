package sbnz.integracija.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import sbnz.integracija.example.facts.Tag;

public interface TagRepository extends JpaRepository<Tag,Long>{

	Tag findByTagName(String name);
	
	@Query(value = "SELECT * FROM Tag t WHERE t.tag_approved = false", nativeQuery = true)
	List<Tag> findTags();

	/*@Modifying(flushAutomatically = true)
	@Query("UPDATE Tag t SET t.tag_approved = true WHERE t.tag_name = :name")
	void setConfirmed(String name);*/

	void delete(Tag entity);
}
